if ("${params.SLAVE_NODE_NAME}" == '') {
    SLAVE_NODE_NAME="${params.SLAVE_LABEL}-${ARCH}"
}
else {
    SLAVE_NODE_NAME="${params.SLAVE_NODE_NAME}"
}

if ( params.OVERCLOUD_DEPLOY == true ) {
    CONTROLLER_NAME=params.CONTROLLER_NAME
    MODEL_NAME=params.MODEL_NAME
} else {
    if ( params.CLOUD_NAME=='ruxton' || params.CLOUD_NAME=='icarus' || params.CLOUD_NAME=='amontons') {
        CLOUD_NAME="${params.CLOUD_NAME}-maas"
    } else { 
        CLOUD_NAME=params.CLOUD_NAME 
    }
    CONTROLLER_NAME="${ARCH}-mosci-${CLOUD_NAME}"
    MODEL_NAME=CONTROLLER_NAME
}
if ( params.CLOUD_NAME.contains("390")) {
        S390X=true
}  else { S390X=false }

if ( params.CLOUD_NAME.contains("ruxton") ) {
    MAAS_API_KEY = params.RUXTON_API_KEY
} else if ( params.CLOUD_NAME.contains("icarus") ) {
    MAAS_API_KEY = params.ICARUS_API_KEY
} else if ( params.CLOUD_NAME.contains("amontons") ) {
    MAAS_API_KEY = params.AMONTONS_API_KEY
}


CONMOD = "${CONTROLLER_NAME}:${MODEL_NAME}"

def get_neutron_machine_id() {
    timeout(60) {
        waitUntil {
            // need max retries here
            try {
                JUJU_MACHINES = sh (
                script: "juju machines|grep -v lxd",
                returnStdout: true
                )
                if ( JUJU_MACHINES.contains("pending")) {
                    echo "Machines not ready"
                    sleep(240)
                    return false
                } else {
                    try {
                        MACHINE_ID = sh (
                        script: "juju status neutron-gateway | awk /started/'{print \$1}'",
                        returnStdout: true
                        )
                        NEUTRON_ID = sh (
                        script: "juju show-machine ${MACHINE_ID} | awk /instance-id/'{print \$2}' | head -n1",
                        returnStdout: true
                        )
                    if ( NEUTRON_ID == '' ) {
                        echo "Neutron ID error: ${NEUTRON_ID}"
                        sh "juju  status -m ${CONMOD}"
                        currentBuild.result = 'FAILURE'
                        return true
                    } else {
                        echo "Got neutron machine id: ${NEUTRON_ID}"
                        return true
                    }
                    } catch (error) {
                        echo "Error getting neutron gateway machine id"
                        currentBuild.result = 'FAILURE'
                        return true
                    }
                }
            } catch (error) {
                echo "Error trying to get juju machines"
                currentBuild.result = 'FAILURE'
                return true
            }
        }
    }
}
def get_neutron_interfaces() {
        get_neutron_machine_id()
        maas_api_cmd = "-o ${params.MAAS_OWNER} -m ${CLOUD_NAME} -k ${MAAS_API_KEY} --interfaces ${NEUTRON_ID}"
        dir("${env.HOME}/tools/openstack-charm-testing/") {
                try {
                    NEUTRON_INTERFACES = sh (
                    script: "./bin/maas_actions.py ${maas_api_cmd}",
                    returnStdout: true
                    )
                    if ( NEUTRON_INTERFACES == '') {
                        echo "No neutron interfaces found"
                    } else {
                        echo "Found interfaces:\n ${NEUTRON_INTERFACES}"
                    }
                } catch (error) {
                        echo "Error getting neutron interfaces, ${error}"
                        currentBuild.result = 'FAILURE'
                }
        }
}


node("${SLAVE_NODE_NAME}") {
    ws("${params.WORKSPACE}") {
        stage('Get bundle') {
            echo "SLAVE_NODE_NAME: ${SLAVE_NODE_NAME}"
            echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
            echo "S390X: ${S390X}"
            echo "CLOUD_NAME: ${CLOUD_NAME}"
            OVERLAY_STRING = ""
            if (fileExists('bundle.yaml')) {
                sh "rm bundle.yaml"
            }
            try {
                sh "curl ${params.BUNDLE_URL} -o bundle.yaml"
                sh "cat bundle.yaml"
                // return true
            } catch (error) {
                echo "Full bundle paste:"
                writeFile file: "bundle.yaml", text: params.BUNDLE_PASTE
            } 
            if ( params.BUNDLE_OVERLAYS != '' ) {
                OVERLAYS = params.BUNDLE_OVERLAYS.split(',|\n')
                echo "Overlays found: ${OVERLAYS}"
                OVERLAY_COUNT = OVERLAYS.size()
                for ( int i = 0 ; i < OVERLAY_COUNT ; i++ ) { 
                        filename = OVERLAYS[i].split('/').last()
                        sh "curl ${OVERLAYS[i]} -o overlay_${i}.yaml"
                        OVERLAY_STRING = OVERLAY_STRING + "--overlay overlay_${i}.yaml "
                }
            }
        }
        //stage('Validate bundle') {
        //    sh "yamllint bundle.yaml"
        //}
        retries = 0
        stage('Modify bundle') {
            echo "Make changes to bundle according to MODIFY_BUNDLE"
            if ( params.MODIFY_BUNDLE != "" ) {
                CONFIG_LINES = params.MODIFY_BUNDLE.split('\n')
                for ( int i = 0 ; i < CONFIG_LINES.size() ; i++ ) {
                    FIND = CONFIG_LINES[i].split(',')[0]
                    REPLACE = CONFIG_LINES[i].split(',')[1]
                    try {
                        FIND_REPLACE = sh (
                            script: "sed -i \"s|${FIND}|${REPLACE}|g\" bundle.yaml",
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "WARNING: error overriding bundle config: ${error}"
                    }
                }
            }
        }
        stage('Deploy bundle') {
            waitUntil {
                try {
                    sh "juju deploy bundle.yaml --map-machines=existing --model=${CONMOD} ${OVERLAY_STRING}"
                    return true
                } catch (error) {
                    if ( params.MANUAL_JOB == true ) {
                        echo "Deploy error. You can ssh into ${OPENSTACK_PUBLIC_IP} and manually modify ${params.WORKSPACE}/bundle.yaml if necessary."
                        msg_one = "Deployment error detected"
                        name = "Abort, Ignore (continue to next stage), or Retry.\nSelect Ignore or Retry and click Proceed, or click the Abort button.\n"
                        def returnValue = input message: msg_one, parameters: [choice(choices: 'retry\nignore', description: '', name: name)]
                        if ( returnValue == "retry" ) {
                            return false
                        } else if ( returnvalue == "ignore" ) {
                            return false
                        }
                    } else {
                        retries++
                        if ( retries == 3 ) {
                            echo "Exceeded retry limit"
                            currentBuild.result = 'FAILURE'
                            error "Exceeded retry limit"
                            return true
                        } else {
                            echo "Bundle deploy error, retrying max. 3 times"
                            sleep(10)
                            return false
                        }
                    }
                }
            }
        }
        stage('Override bundle config') {
            echo "Deprecated - use MODIFY_BUNDLE"
            if ( params.OVERRIDE_BUNDLE_CONFIG != "" ) {
                CONFIG_LINES = params.OVERRIDE_BUNDLE_CONFIG.split('\n')
                for ( int i = 0 ; i < CONFIG_LINES.size() ; i++ ) {
                    APP = CONFIG_LINES[i].split(',')[0]
                    CONFIG = CONFIG_LINES[i].split(',')[1]
                    try {
                        JUJU_CONFIG = sh (
                            script: "juju config ${APP} ${CONFIG}",
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "WARNING: error overriding bundle config: ${error}"
                    }
                }
            }
        }
        stage('Configure neutron gateway dataport') {
            if ( params.OPENSTACK && S390X == false && ! params.OVERCLOUD_DEPLOY == true ) {
                if ( params.NEUTRON_DATAPORT != "" ) {
                    NEUTRON_INTERFACES = params.NEUTRON_DATAPORT
                } else {
                    get_neutron_interfaces()
                    NEUTRON_INTERFACE = NEUTRON_INTERFACES.split('\n')
                    // echo "Found neutron interfaces in this stage: ${NEUTRON_INTERFACES}"
                }
                try {
                        echo "Waiting for neutron-gateway to settle so that we can ensure data-port config is correct"
                        sleep(60)
                        sh (
                            script: "juju config neutron-gateway data-port=\"br-ex:${NEUTRON_INTERFACE[-1]}\"",
                            returnStdout: true
                        )
                        echo "Resolve neutron-gateway errors if there are any after setting correct data-port"
                        sleep(60)
                        sh (
                            script: "for a in \$(juju status neutron-gateway|grep -E 'neutron.*error'|awk '!/jujucharms/ {print \$1}'|tr -d '*'); do echo juju resolved \$a ; juju resolved \$a ; done",
                            returnStdout: true
                        )
                } catch (error) {
                        echo "Error setting neutron-gateway data-port: ${error}"
                }
            } else {
                echo "This is not an openstack deployment or is s390x, not configuring neutron gateway dataport"
            }
        }
        stage('Executing post deploy commands') {
            if ( params.POST_DEPLOY_CMD != "" ) {
                echo "Executing: ${params.POST_DEPLOY_CMD}"
                try {
                    PDCMD = sh (
                        script: params.POST_DEPLOY_CMD,
                        returnStdout: true
                    ).trim()
                echo "POST_DEPLOY_CMD: ${PDCMD}"
                } catch (error) {
                    echo "Error running POST_DEPLOY_CMD: ${error}" 
                } 
            } else {
                echo "POST_DEPLOY_CMD undefined, skipping"
            }
        }
        stage('Wait for deploy to complete') {
            echo "Waiting for deployment to complete..."
            sleep(60)
            try {
                timeout(params.DEPLOY_TIMEOUT.toInteger()) {
                    sh "~/tools/juju-wait/juju-wait -e ${CONMOD}"
                }
            } catch (error) {
                echo "juju-wait has failed or timed out."
                //input "continue stages, or abort?"
                currentBuild.result = 'FAILURE'
            }
            try {
                CHECK_STATUS = sh (
                script: "juju status -m ${CONMOD}",
                returnStdout: true
                )
            if ( CHECK_STATUS.contains("error") ) {
                echo "${CHECK_STATUS}"
                currentBuild.result = 'FAILURE'
                error
            } else {
                echo CHECK_STATUS
                }
            } catch (error) {
                currentBuild.result = 'FAILURE'
                error "FAILURE: error found in juju status"
            }
        }
    }
}

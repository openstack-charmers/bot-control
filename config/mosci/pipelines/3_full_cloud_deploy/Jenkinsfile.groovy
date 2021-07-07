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
    if ( params.CLOUD_NAME=='ruxton' || params.CLOUD_NAME=='icarus' ) {
        CLOUD_NAME="${params.CLOUD_NAME}-maas"
    } else { 
        CLOUD_NAME=params.CLOUD_NAME 
    }
    CONTROLLER_NAME="${ARCH}-mosci-${CLOUD_NAME}"
    if ( params.CLOUD_NAME == "lxd" ) {
        MODEL_NAME="${ARCH}-mosci-${CLOUD_NAME}-${LXD_IP}".replaceAll("[.]", "-")
    } else {
    MODEL_NAME=CONTROLLER_NAME
    }
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

/*def get_bundle_repo(GUESS_REPO) {
    if ( params.BUNDLE_REPO) {
    
        bundle_repo = params.BUNDLE_REPO.split(',')[0]
        bundle_repodir = params.BUNDLE_REPO.split(',')[1]
    
        try {
            sh "git clone ${BUNDLE_REPO} ~/bundle_repo/"
        } catch (error) {
            echo "Full bundle paste:"
            writeFile file: "bundle.yaml", text: params.BUNDLE_PASTE
        }
    }
}

def zaza_config_check(GUESS_REPO_DIR) {
    // check if the tests.yaml in the bundle dir contains zaza config steps
    // if it does, we will configure the job with zaza and also attempt to run zaza tests
    // if it does not, we will do legacy configuration, and run selected tests
    tests_yaml = readFile("${env.HOME}/bundle_repo/openstack_bundles/${GUESS_REPO_DIR}/tests/tests.yaml")  
    if tests_yaml.contains('configure: []') {
        return false
    } else {
        return true
    }
}*/


CONMOD = "${CONTROLLER_NAME}:${MODEL_NAME}"

def get_neutron_machine_id(NETWORK_APP) {
    timeout(60) {
        waitUntil {
            // need max retries here
            try {
                JUJU_MACHINES = sh (
                script: "juju machines -m ${CONMOD} |grep -v lxd",
                returnStdout: true
                )
                if ( JUJU_MACHINES.contains("pending")) {
                    echo "Machines not ready"
                    sleep(240)
                    return false
                } else {
                    try {
                        NEUTRON_ID = sh (
                        script: "juju show-machine \$(juju status ${NETWORK_APP} -m ${CONMOD}| awk /started/'{print \$1}') | awk /instance-id/'{print \$2}' | head -n1",
                        returnStdout: true
                        )
                    if ( NEUTRON_ID == '' ) {
                        echo "Neutron ID error: ${NEUTRON_ID}"
                        sh "juju status -m ${CONMOD}"
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
def get_neutron_interfaces(NETWORK_APP) {
        get_neutron_machine_id(NETWORK_APP)
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
            /*
            // Try to get the BUNDLE_REPO and see if there are any zaza configuration steps
            // This only currently works with the openstack-bundles repo
            GUESS_REPO = BUNDLE_URL.split('/')[0..4].join('/').replace('raw.githubusercontent.com','www.github.com')
            GUESS_REPO_DIR = BUNDLE_URL.split('/')[8..6].reverse().join('/')
            try {
                get_bundle_repo(GUESS_REPO)
                if ( zaza_config_check(GUESS_REPO_DIR) ) {
                    ZAZA = true
                    params.ZAZA = true
                }
            } catch (error) {
                echo "Could not clone bundle repo ${GUESS_REPO} - bad guess?"
            }*/

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
                    sh "juju deploy ./bundle.yaml --map-machines=existing --model=${CONMOD} ${OVERLAY_STRING} --force"
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
                            script: "juju config ${APP} ${CONFIG} -m ${CONMOD}",
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "WARNING: error overriding bundle config: ${error}"
                    }
                }
            }
        }
        stage('Configure neutron gateway dataport') {
            if ( params.OPENSTACK && ! params.OVERCLOUD_DEPLOY == true ) {
                try {
                    CHECK_OVN = sh (
                            script: "juju status ovn-chassis",
                            returnStdout: true 
                        )
                } catch(error) {
                    echo "Could not get juju status: ${error}"
                }
                if ( CHECK_OVN.contains("active")) {
                    echo "OVN deployment detected"
                    NETWORK_APP = "ovn-chassis"
                    APP_PREX = "ovn"
                    NETWORK_CONF = 'bridge-interface-mappings'
                } else {
                    echo "Neutron gateway deployment detected"
                    NETWORK_APP = "neutron-gateway"
                    APP_PREX = "neutron"
                    NETWORK_CONF = "data-port"
                }
            }
            if ( params.OPENSTACK && S390X == false && ! params.OVERCLOUD_DEPLOY == true ) {
                if ( params.NEUTRON_DATAPORT != "" ) {
                    NEUTRON_INTERFACES = params.NEUTRON_DATAPORT
                } else {
                    get_neutron_interfaces(NETWORK_APP)
                    NEUTRON_INTERFACE = NEUTRON_INTERFACES.split('\n')
                    // echo "Found neutron interfaces in this stage: ${NEUTRON_INTERFACES}"
                }
                try {
                        echo "Waiting for ${NETWORK_APP} to settle so that we can ensure ${NETWORK_CONF} is correct"
                        sleep(60)
                        sh (
                            script: "juju config ${NETWORK_APP} ${NETWORK_CONF}=\"br-ex:${NEUTRON_INTERFACE[-1]}\" -m ${CONMOD}",
                            returnStdout: true
                        )
                        echo "Resolve ${NETWORK_APP} errors if there are any after setting correct ${NETWORK_CONF}"
                        sleep(60)
                        sh (
                            script: "for a in \$(juju status ${NETWORK_APP} -m ${CONMOD} |grep -E '${APP_PREX}.*error'|awk '!/jujucharms/ {print \$1}'|tr -d '*'); do echo juju resolved \$a -m ${CONMOD}; juju resolved \$a -m ${CONMOD}; done",
                            returnStdout: true
                        )
                } catch (error) {
                        echo "Error setting ${NETWORK_APP} ${NETWORK_CONF}: ${error}"
                }
            } else {
                echo "This is not an openstack deployment or is s390x, not configuring ${NETWORK_APP} ${NETWORK_CONF}"
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

if ( params.S390X_NODES == null || params.S390X_NODES == "none") {
    S390X_NODES = ""
} else {
    S390X_NODES = params.S390X_NODES
}

def s390x_rm_swift_img(reset_machine) {
    echo "Cleaning /mnt/swift/*"
    try {
        sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${reset_machine} \"sudo rm -rf /mnt/swift/*\""
    } catch (error) {
        echo "Erroring with rm -rf /mnt/swift/*: ${error}"
    }
}

def s390x_snapshot_reset(reset_machine) {
        echo "Attempting to restore an LVM snapshot. If we do not find one, we will create one."
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        //for (int i = 0; i < reset_machines.size(); i++ ) { 
            try {
                sh "scp ./bin/snap_root_util.py ubuntu@${reset_machine}:/home/ubuntu/"
                def check_snapshot = sh (
                        script: "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${reset_machine} /home/ubuntu/snap_root_util.py --restore",
                        returnStatus: true
                )
                if ( check_snapshot == 0 ) {
                    echo "Restore OK, rebooting machine and creating a new snapshot"
                    sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${reset_machine} sudo shutdown -r 1"
                    sleep(120)
                    s390x_snapshot_create(reset_machine)
                    return true 
                } else if ( check_snapshot == 1 ) {
                    echo "Restore in progress, attempting to create snapshot"
                    s390x_snapshot_create(reset_machine)
                } else if ( check_snapshot == 2 ) {
                    echo "No snapshot found, aborting"
                    // s390x_snapshot_create(reset_machine)
                    currentBuild.result = 'FAILURE'
                    // error "No snapshot found but machine is already provisioned - cannot recover automatically"
                    error "No snapshot found"
                    return false
                }
            echo "Unhandled exception: ${check_snapshot}"
            return false
            } catch (error) {
            if ( error.message == "No snapshot found" ) { 
                echo "No snapshot found but machine is already provisioned - cannot recover automatically"
                currentBuild.result = 'FAILURE'
                error "FAILED BUILD"
                return false
            } else {
                echo "SSH error, retrying"
                sleep(60)
                return true 
                }
            }
        //}
    }
}

def s390x_snapshot_create(snapshot_machine) {
        echo "Attempting to create an LVM snapshot..."
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        // for (int i = 0; i < snapshot_machines.size(); i++ ) { 
                sh "scp ./bin/snap_root_util.py ubuntu@${snapshot_machine}:/home/ubuntu/"
                // this block will try forever to create a snapshot. MAX_RETRIES needed here.
                waitUntil {
                    try {
                        create_cmd = sh (
                        script: "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${snapshot_machine} /home/ubuntu/snap_root_util.py --check",
                        returnStatus: true
                    )
                    if ( create_cmd == 0 ) {
                    echo "Snapshot found, proceeding."
                    return true
                    } else if ( create_cmd == 1 ) {
                        echo "Existing snapshot found, restore in progress"
                        sleep(120)
                        return false 
                    } else if ( create_cmd == 2 ) {
                        echo "No snapshot found, creating" 
                        try {
                            sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${snapshot_machine} /home/ubuntu/snap_root_util.py --create"
                            sleep(60)
                            return false
                        } catch (inner_error) {
                               echo "Error with snap root creation, failing build: ${inner_error}"
                               currentBuild.result = 'FAILURE'
                               return true
                        }
                    }
                    echo "Unhandled error: ${create_cmd}"
                    return false
                    } catch (error) {
                        echo "Machine may not be reachable, trying again... ${create_cmd}"
                        sleep(120)
                        return false
                    }
                    }
                //}
        }
}

if ( params.MODEL_CONSTRAINTS.contains("arch=") ) {
        echo "Overriding ARCH param with model constraint specific architecture"
}
else {
        MODEL_CONSTRAINTS="arch=${params.ARCH} ${params.MODEL_CONSTRAINTS}"
        env.MODEL_CONSTRAINTS="arch=${params.ARCH} ${params.MODEL_CONSTRAINTS}"
}

echo "MODEL_CONSTRAINTS=${MODEL_CONSTRAINTS}"


if ( "${params.BOOTSTRAP_CONSTRAINTS}".contains("arch=") ) {
        echo "Overriding ARCH param with bootstrap constraint specific architecture"
}
else {
        BOOTSTRAP_CONSTRAINTS="arch=${params.ARCH} ${params.BOOTSTRAP_CONSTRAINTS}"
        env.BOOTSTRAP_CONSTRAINTS="arch=${params.ARCH} ${params.BOOTSTRAP_CONSTRAINTS}"
}

echo "BOOTSTRAP_CONSTRAINTS=${BOOTSTRAP_CONSTRAINTS}"

if ( params.CLOUD_NAME=='ruxton' || params.CLOUD_NAME=='icarus' || params.CLOUD_NAME=='amontons' ) {
                CLOUD_NAME="${params.CLOUD_NAME}-maas"
                env.CLOUD_NAME="${params.CLOUD_NAME}-maas"
                MAAS=true
} else { MAAS=false }
if ( params.CLOUD_NAME.contains("390")) {
        S390X=true
}  else { S390X=false }
if ( params.CLOUD_NAME=='lxd' ) {
        CONTROLLER_NAME="${params.ARCH}-mosci-${CLOUD_NAME}" + LXD_IP.replaceAll("[.]", "-") 
        MODEL_NAME="lxd-${LXD_IP}".replaceAll("[.]", "-")

}

CONMOD = "${CONTROLLER_NAME}:${MODEL_NAME}"

def s390x_add_machine(add_machines) {
    echo "s390x_add_machine"
    for (int i = 0; i < add_machines.size(); i++ ) { 
        timeout(60) {
            try {
                waitUntil {
                    def check_model = sh (
                        script: "juju status -m ${CONMOD} > juju_model",
                        returnStatus: true
                    )
                    if ( check_model != 0 ) {
                        echo "Something went wrong getting juju status: ${check_model}"
                    }
                    juju_model = readFile('juju_model').trim()
                    if ( juju_model.contains("${add_machines[i]}") ) {
                        echo "${add_machines[i]} already in model, skipping" 
                        return true
                    }
                    check_retries = 0
                    waitUntil {
                        if ( check_retries == 1 ) {
                            echo "Already rebooted ${add_machines[i]}, skipping"
                            return true
                        }
                        try {
                            def check_snap_root = sh (
                                script: "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${add_machines[i]} \"sudo lvs -a|grep snap-root\"",
                                returnStatus: true
                            )
                            if ( check_snap_root == 255 ) {
                                echo "SSH problem, exitcode: ${check_snap_root}, trying again"
                                sleep(300)
                                return false
                            } else if ( check_snap_root == 1 ) {
                                try {
                                    check_retries = 1
                                    sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@${add_machines[i]} \"sudo reboot\""
                                } catch (error) {
                                    echo "Handling ssh reboot error code: ${error}"
                                }
                                echo "Waiting for reboot"
                                sleep(60)
                                return false
                            } else if ( check_snap_root == 0 ) { 
                                echo "snap-root found on ${add_machines[i]}"
                                return true
                            }
                        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException error_flow) {
                            echo "Timed out attempting to add node, failing build."
                            currentBuild.result = 'FAILURE'
                            error "FAILED"
                            return true
                        }
                    } 
                    def exitcode = sh (
                        script: "#!/bin/bash \nset -o pipefail ; juju add-machine ssh:ubuntu@${add_machines[i]} --debug 2>&1 | tee add_machines",
                        returnStatus: true
                        )
                    if ( exitcode == 0 ) {
                        //echo "exitcode ok is ${exitcode}"
                        s390x_rm_swift_img(add_machines[i])
                        return true
                    } else {
                        error = readFile('add_machines').trim()
                        if ( error.contains("ERROR machine is already provisioned" ) ) {
                            echo "exitcode already provisioned is ${exitcode}, PRE_RELEASE_MACHINES = ${PRE_RELEASE_MACHINES}"
                            if ( PRE_RELEASE_MACHINES == 'true' ) {
                                echo "Machine is already provisioned, PRE_RELEASE_MACHINES is true, releasing..."
                                s390x_rm_swift_img(add_machines[i])
                                def mac_res = s390x_snapshot_reset(add_machines[i])
                                if ( ! mac_res ) { return true }
                                    return false
                                } else {
                                    echo "Machine is already provisioned, but PRE_RELEASE_MACHINES is not true. Aborting."
                                    currentBuild.result = 'FAILED'
                                    error "FAILED"
                                    return true
                                } 
                        } else if ( error.contains("Host key verification failed") ) {
                            currentBuild.result = 'FAILURE'
                            error "Problem with host key, aborting build as out of order machines may cause bundle issues"
                        } else if ( error.contains("No route to host") || error.contains("Connection refused") ) {
                            echo "${add_machines[i]} is not ready, waiting before retrying" 
                            echo "max retries = infinite"
                            sleep(120)
                            return false
                        } else if ( error.contains("Permission denied") ) {
                            echo "Permission denied, probably rebuilding from preseed, retrying"
                            sleep(120)
                            return false
                        } else if ( error.contains("Connection timed out") ) {
                            echo "Connection timed out, perhaps redeploying?"
                            sleep(120)
                            return false
                        }
                    echo "some other error with attempting to add ${add_machines[i]} - perhaps redeploying?"
                    echo "exitcode is ${exitcode}"
                    currentBuild.result = 'FAILURE'
                    error "unhandled exception"
                    return true 
                    }
                }
            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException error) {
                echo "Timed out attempting to add nodes, failing build."
                currentBuild.result = 'FAILURE'
                error "FAILED"
            }
        }
    }
}

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) { 
        stage('Source things and get variables') {
            dir("${env.HOME}/tools/openstack-charm-testing") {
                OS_PROJECT_NAME="${params.ARCH}-mosci"
                CONTROLLER_NAME="${params.ARCH}-mosci-${CLOUD_NAME}"
                MODEL_NAME="${params.ARCH}-mosci-${CLOUD_NAME}"
                BOOTSTRAP_LOCAL=Boolean.valueOf(params.BOOTSTRAP_ON_SLAVE)
                if ( params.OVERCLOUD_DEPLOY ) {
                    env.OS_PROJECT_NAME=MODEL_NAME
                }
            }
        }
        stage('Pre-release machines') {
            if ( MAAS && ! params.OVERCLOUD_DEPLOY ) {
                    echo "MAAS: ${MAAS} params.OVERCLOUD: ${params.OVERCLOUD_DEPLOY}"
                    TAGS = MODEL_CONSTRAINTS.minus("arch=" + params.ARCH + " ")
                    print "Tags 1: ${TAGS}"
                    TAGS = TAGS.minus("tags=")
                    TAGS = TAGS.replace(" ", "")
                    TAGS = TAGS.split(",")
                    print "Tags 2: ${TAGS}"
                    if ( params.CLOUD_NAME.contains("ruxton") ) {
                            MAAS_API_KEY = params.RUXTON_API_KEY 
                    } else if ( params.CLOUD_NAME.contains("icarus") ) {
                            MAAS_API_KEY =params.ICARUS_API_KEY
                    } else if ( params.CLOUD_NAME.contains("amontons") ) {
                            MAAS_API_KEY = params.AMONTONS_API_KEY
                    }
                    primary_tag = TAGS[0]
                    additional_tags = TAGS.join(",")
                    if ( params.PRE_RELEASE_MACHINES ) {
                        stage("MAAS pre-release nodes: ${params.ARCH}, ${TAGS}") {
                                def maas_api_cmd = ""
                                echo "Primary tag: ${primary_tag}, additional_tags: ${additional_tags}, release nodes: ${params.PRE_RELEASE_MACHINES}, force: ${params.FORCE_RELEASE}"
                                maas_api_cmd = maas_api_cmd + "-o ${params.MAAS_OWNER} -m ${CLOUD_NAME} -k ${MAAS_API_KEY} --release --tags ${primary_tag} --arch ${params.ARCH}"
                        if ( primary_tag != additional_tags ) { 
                                echo "not same"
                                maas_api_cmd = maas_api_cmd + " --additional ${additional_tags} "
                        }
                        if ( params.FORCE_RELEASE )  {
                                echo "force true"
                                maas_api_cmd = maas_api_cmd + " --force "
                        } 
                        dir("${env.HOME}/tools/openstack-charm-testing/") {
                                sh "./bin/maas_actions.py ${maas_api_cmd}"
                                echo "Waiting for machines to release"
                                sleep(60)
                        }
                    }
                }
            } 
        }
        stage('Bootstrap controller') {
            timeout(params.BOOTSTRAP_TIMEOUT.toInteger()) {
                echo "${env.SLAVE_NODE_NAME}"
                echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
                dir("${env.HOME}/cloud-credentials") {
                    echo "Ensure cloud-credentials directory exists"
                    sh "touch .placeholder"
                }
                dir("${env.HOME}/tools/charm-test-infra") {
                    if ( params.OVERCLOUD_DEPLOY ) { CONTROLLER_NAME = params.CONTROLLER_NAME }
                    try {
                        CHECK_JUJU = sh (
                            script: "juju controllers",
                            returnStdout: true
                        )
                        if ( CHECK_JUJU.contains(CONTROLLER_NAME) ) {
                            echo "Found existing controller ${CONTROLLER_NAME}."
                            EXISTING_CONTROLLER = true
                        } else { 
                            EXISTING_CONTROLLER = false 
                        }
                    } catch (error) {
                        echo "WARNING: no existing juju controllers found."
                        EXISTING_CONTROLLER = false
                    }
                    if ( params.KEYSTONE_AUTH_URL == '' ) {
                            KSXCMD = "true"
                    } else { KSXCMD = "export OS_AUTH_URL=${params.KEYSTONE_AUTH_URL}" }
                    if ( ! S390X && EXISTING_CONTROLLER && ( params.PRE_RELEASE_MACHINES && BOOTSTRAP_CONSTRAINTS.contains(ARCH) ) || ( params.FORCE_NEW_CONTROLLER ) ) {
                        if ( params.FORCE_NEW_CONTROLLER) { 
                            echo "INFO: FORCE_NEW_CONTROLLER = true" 
                        } else {
                            echo "Existing controller found, PRE_RELEASE_MACHINES is true, BOOTSTRAP_CONSTRAINTS contains ${ARCH}"
                            echo "This means you have probably released the controller machine, so we will run kill-controller."
                        }
                        try {
                            KILL_C = sh (
                                script: "juju kill-controller ${CONTROLLER_NAME} -y",
                                returnStdout: true
                            )
                        } catch (error) {
                            echo "Error killing controller ${CONTROLLER_NAME}: ${error}"
                        }
                    }
                    if ( params.OVERCLOUD_DEPLOY ) {
                        echo "OVERCLOUD_DEPLOY is true"
                        SRCCMD = "#!/bin/bash\nsource ${env.HOME}/tools/openstack-charm-testing/rcs/openrc"
                        // create a clouds.yaml for this overcloud with keystone endpoint
                        // env.OS_PROJECT_NAME=params.CLOUD_NAME
                        // env.CLOUD_NAME="overcloud"
                        // env.OS_PROJECT_NAME=MODEL_NAME
                        BOOTSTRAP_CONSTRAINTS="arch=${ARCH}"
                        MODEL_CONSTRAINTS="arch=${ARCH}"
                        // env.OS_PROJECT_NAME="${params.ARCH}-mosci"
                        MODEL_NAME=params.MODEL_NAME
                        echo "MODEL_NAME: ${MODEL_NAME}, CONTROLLER_NAME: ${CONTROLLER_NAME}"
                        sh "cp juju-configs/clouds.yaml ${env.HOME}/cloud-credentials/"
                        sh "sed -i '0,/serverstack:/s/serverstack:/overcloud:/g' juju-configs/clouds.yaml"
                        sh "sed -i '0,/serverstack:/s/serverstack:/RegionOne:/g' juju-configs/clouds.yaml"
                        sh "cat juju-configs/clouds.yaml"
                        sh "juju switch ${OVERCLOUD_NAME}:${OVERCLOUD_NAME}"
                        echo "${KSXCMD}"
                        sh "${SRCCMD} ; ./juju-openstack-controller-mosci.sh"
                        sh "cp ${env.HOME}/cloud-credentials/clouds.yaml juju-configs/"
                    } else {
                        env.MODEL_NAME=MODEL_NAME
                        echo "MODEL_NAME: ${env.MODEL_NAME}, CONTROLLER_NAME: ${env.CONTROLLER_NAME}"
                        env.OS_PROJECT_NAME="${params.ARCH}-mosci"
                        env.CLOUD_NAME=CLOUD_NAME
                        env.CONTROLLER_NAME=CONTROLLER_NAME
                        env.MODEL_NAME=MODEL_NAME
                        sh "./juju-maas-controller-mosci.sh"
                    }
                }
            }
            echo "Exited bootstrap timeout block"
        }
        stage('Override controller config') {
            if ( params.OVERRIDE_CONTROLLER_CONFIG != "" ) {
                CONFIG_LINES = params.OVERRIDE_CONTROLLER_CONFIG.split('\n')
                for ( int i = 0 ; i < CONFIG_LINES.size() ; i++ ) {
                    try {
                        JUJU_CONFIG = sh (
                            script: "juju controller-config ${CONFIG_LINES[i]} -c ${CONTROLLER_NAME}",
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "WARNING: error overriding controller config: ${error}"
                    }
                }
            }
        }
        stage('Override model config') {
            if ( params.OVERRIDE_MODEL_CONFIG != "" ) {
                CONFIG_LINES = params.OVERRIDE_MODEL_CONFIG.split('\n')
                for ( int i = 0 ; i < CONFIG_LINES.size() ; i++ ) {
                    try {
                        JUJU_CONFIG = sh (
                            script: "juju model-config ${CONFIG_LINES[i]} -m ${CONMOD}",
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "WARNING: error overriding model config: ${error}"
                    }
                }
            }
        }
        if ( S390X || S390X_NODES != '' ) {
            echo "S390X, S390X_NODES: ${S390X}, ${S390X_NODES}"
            stage('Add s390x lpars') {
                timeout(90) {
                    machines = S390X_NODES.split(',')
                    if ( machines != '' ) {
                            echo "s390x lpars: ${S390X_NODES}"
                    } else { 
                        currentBuild.result='FAILURE'
                        error "No s390x lpars selected for this deployment, but cloud is s390x. Aborting."
                    }
                    if ( params.PRE_RELEASE_MACHINES == 'true' ) {
                        echo "Pre-releasing machines: ${machines}"
                        s390x_snapshot_remove(machines)                        
                    }
                    echo "Adding machines: ${machines}"
                    s390x_add_machine(machines)
                }
                echo "Exited add lpar timeout block"
            }
        }
    }
}

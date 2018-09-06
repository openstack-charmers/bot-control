// This method collects a list of Node names from the current Jenkins instance
@NonCPS
def nodeNames() {
  return jenkins.model.Jenkins.instance.nodes.collect { node -> node.name }
}

def names = nodeNames()

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
                echo "check_snapshot is ${check_snapshot}"
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
                    echo "No snapshot found, skipping"
                    return false
                }
            echo "Unhandled exception: ${check_snapshot}"
            return false
            } catch (error) {
            echo "SSH error, retrying"
            sleep(60)
            return false
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
                        sleep(60)
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

if ( "${params.ARCH}".contains("s390x") ) { 
        echo "s390x arch, not maas"
        CONTROLLER_NAME="${params.ARCH}-mosci-${params.CLOUD_NAME}"
        MODEL_NAME=CONTROLLER_NAME
} else if ( params.OVERCLOUD_DEPLOY ) {
        echo "OVERCLOUD_DEPLOY == true"
        CONTROLLER_NAME=params.CONTROLLER_NAME
        MODEL_NAME=params.MODEL_NAME
} else {
        CONTROLLER_NAME="${params.ARCH}-mosci-${params.CLOUD_NAME}-maas"
        MODEL_NAME=CONTROLLER_NAME
        echo "MAAS detected, setting controller and model names: ${CONTROLLER_NAME}, ${MODEL_NAME}"
}

if ( params.SLAVE_NODE_NAME == '') {
    // This is not valid, since the master doesnt have capabilities to release machines
    ONLY_RELEASE = true
    echo "Slave name not set, will only attempt to release MAAS nodes"
} else {
        ONLY_RELEASE = false
        echo "Hunting: ${CONTROLLER_NAME} on ${params.SLAVE_NODE_NAME}"
        
        echo "All node names: " + names
        if ( names.contains(params.SLAVE_NODE_NAME) ) {
                echo "${params.SLAVE_NODE_NAME} found..."
        } else { 
                echo "${params.SLAVE_NODE_NAME} not found, aborting"
                currentBuild.result = 'FAILURE'
                return
        }
}

if ( params.SLAVE_NODE_NAME == 'master' ) {
        echo "Refusing to target master node, aborting"
        currentBuild.result = 'FAILURE'
        return
} 



if ( ONLY_RELEASE != true ) { 
echo "Attempting to connect to ${params.SLAVE_NODE_NAME}"
        for (aSlave in hudson.model.Hudson.instance.slaves) {
                if ( aSlave.name == params.SLAVE_NODE_NAME ) {
                        def computer = aSlave.computer
                        if ( aSlave.getComputer().isOffline() ) {
                                echo "${params.SLAVE_NODE_NAME} is offline, bringing it online to delete juju models/controllers"
                                computer.connect(true)
                        }
                }
        }
        try {
            timeout(90) {
                node(params.SLAVE_NODE_NAME) {
                    ws("${params.WORKSPACE}") {
                        stage("Archive juju logs") {
                            //if ( params.CRASHDUMP ) {
                                try {
                                    sh "juju switch ${MODEL_NAME}"
                                    sh "mkdir -p crashdumps ; /snap/bin/juju-crashdump -o crashdumps/${MODEL_NAME}-${BUILD_ID}.tar.xz"
                                    archiveArtifacts 'crashdumps/*'
                                } catch (error) {
                                    echo "Error collecting crashdump logs, stopping teardown in case manual log collection is required"
                                    currentBuild.result = 'FAILURE'
                                    error "FAILURE"
                                }
                            //}
                        }
                        stage("juju teardown") {
                            echo "SLAVE_NODE_NAME: ${params.SLAVE_NODE_NAME}"
                            echo "OPENSTACK_PUBLIC_IP = ${env.OPENSTACK_PUBLIC_IP}"
                            echo "Resolving any errored hooks before destroying controller"
                            echo "Max 5 retries"
                            retries = 0 
                            waitUntil { 
                                if ( retries == 5 ) {
                                    echo "Max retries reached, skipping."
                                    return true
                                } else {
                                    echo "Retry: ${retries}" 
                                }
                                try {
                                    CHECK_ERR = sh ( 
                                        script: "juju status --relations=false|grep -E '/.*error'",
                                        returnStatus: true 
                                    )
                                    if ( CHECK_ERR == 0 ) {
                                        RESOLVING = sh (
                                            script: "for a in \$(juju status --relations=false|grep -E '/.*error'|awk '{print \$1}'|tr -d '*'); do juju resolved --no-retry \$a ; juju remove-unit \$a ; done",
                                            returnStdout: true
                                        ).trim()
                                        echo "Resolving: ${RESOLVING}"
                                        sleep(60)
                                        echo "Checking for errors again..."
                                        retries++
                                        return false
                                    } else {
                                        return true
                                    }
                                } catch(error) {
                                    echo "Error checking or resolving broken hooks: ${error}"
                                    echo "controller may not be successfully destroyed within the timeout period."
                                    return true 
                                }
                            }
                            if ( "${params.DESTROY_CONTROLLER}" == "true" ) {
                                echo "Destroying controller and models: ${CONTROLLER_NAME}"
                                try {
                                        sh "juju destroy-controller ${CONTROLLER_NAME} --destroy-all-models -y"
                                } catch (error) {
                                        echo "An error occured:" + error
                                } 
                                MACHINES_RELEASED = true
                            } else { 
                                echo "Not destroying controller..." 
                                if ( "${params.DESTROY_MODEL}" == "true" ) {
                                    echo "Destroying model ${MODEL_NAME}, leaving controller"
                                    try {
                                        sh "juju destroy-model ${MODEL_NAME} -y"
                                    } catch (error) {
                                        echo "Error destroying model: ${error}"
                                    } 
                                    MACHINES_RELEASED = true
                                } else {
                                    echo "Leaving model and controller up" 
                                }
                            }
                        }
                    }
                    machines = params.S390X_NODES.split(',')
                    if ( machines[0] != "none" && machines[0] != "" && machines[0] != "[]" && params.RELEASE_MACHINES ) {
                        echo "Attempting to restore and recreate LVM snapshot for ${machines}"
                        for (int i = 0; i < machines.size(); i++ ) { 
                            s390x_snapshot_reset(machines[i])           
                            }
                        }
                    if ( CONTROLLER_NAME.contains("maas") && params.RELEASE_MACHINES && ! MACHINES_RELEASED ) {
                        TAGS = MODEL_CONSTRAINTS.minus("arch=" + params.ARCH + " ")
                        TAGS = TAGS.minus("tags=")
                        TAGS = TAGS.replace(" ", "")
                        TAGS = TAGS.split(",")
                        if ( params.CLOUD_NAME == 'ruxton' ) {
                                MAAS_API_KEY = RUXTON_API_KEY 
                        } else if ( params.CLOUD_NAME == 'icarus' ) {
                                MAAS_API_KEY = ICARUS_API_KEY
                               }
                        primary_tag = TAGS[0]
                        additional_tags = TAGS.join(",")
                        stage("MAAS release nodes") {
                            try {
                                MAAS_IDS = sh (
                                    script: "juju machines | grep -v lxd | awk '{print \$4}'",
                                    returnStdout: true
                                ).trim()
                            echo "Releasing: ${MAAS_IDS}"
                            } catch (error){
                                echo "Error getting machines from juju: ${error}"
                            }
                            def maas_api_cmd = ""
                            if ( params.RELEASE_MACHINES ) {
                                MAAS_IDS.split("\n").each { MAAS_ID, count ->
                                    maas_api_cmd = maas_api_cmd + "-o ${params.MAAS_OWNER} -m ${params.CLOUD_NAME}-maas -k ${MAAS_API_KEY} --release --system_id ${MAAS_ID}"
                                }
                                dir("${env.HOME}/tools/openstack-charm-testing/") {
                                        sleep(60)
                                        sh "./bin/maas_actions.py ${maas_api_cmd}"
                                }
                            }
                        }
                    }
                }
            } 
        } catch(error) { echo "Timeout connecting to slave, won't be doing any juju cleanup. You may need to maually release machines." }
        node('master') {
                stage("Offline Slave") {
                        if ( params.OFFLINE_SLAVE ) {
                                for (aSlave in hudson.model.Hudson.instance.slaves) {
                                        if ( aSlave.name == params.SLAVE_NODE_NAME ) {
                                                echo "Offlining ${params.SLAVE_NODE_NAME}"
                                                aSlave.getComputer().setTemporarilyOffline(true,null);
                                        }
                                }
                        }
                }
                stage("Restore label") {
                    for ( node in jenkins.model.Jenkins.instance.nodes ) {
                        if (node.getNodeName().equals(NODE_NAME)) {
                            NEW_LABEL="serverstack-${params.ARCH}"
                            node.setLabelString(NEW_LABEL)
                            node.save()
                            echo "Changing node label from ${OLD_LABEL} to ${NEW_LABEL}"
                        }
                    }
                }
                stage("Delete Slave") {
                        if ( params.DESTROY_SLAVE ) {
                                for (aSlave in hudson.model.Hudson.instance.slaves) {
                                        if ( aSlave.name == params.SLAVE_NODE_NAME ) {
                                                echo "Deleting ${params.SLAVE_NODE_NAME}"
                                                aSlave.getComputer().doDoDelete();
                                        }
                                }
                        }
                }
        }
}        

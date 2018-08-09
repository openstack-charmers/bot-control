if ( ! params.OPENSTACK ) {
    error "This is not an openstack deployment"
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

// Simple test to launch a single openstack instance

if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}

SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        stage('Get OS Env vars') {
                echo "SLAVE_NODE_NAME: ${SLAVE_NODE_NAME}"
                echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
                echo "Here we will source novarcv3_project, and then show all the OS vars,"
                echo "so that a user can copy and paste these into a local file and run"
                echo "openstack commands from anywhere which has access to the machines"
                    try {
                        JUJU_SWITCH = sh (
                            script: "juju switch ${ARCH}-mosci-${CLOUD_NAME}:${ARCH}-mosci-${CLOUD_NAME}",
                            returnStdout: true
                        ) 
                    } catch (error) {
                        echo "Couldn't juju switch: ${error}"
                    } 
                    try {
                        ENVARS = sh (
                            script: "#!/bin/bash \n. ./novarcv3_project ; env|grep OS_",
                            returnStdout: true
                        ) 
                    } catch (error) {
                        echo "Error getting env vars: ${error}"
                    }
                }
        stage("API checks") {
            sh "${SRCCMD} ; openstack catalog list ; openstack image list ; openstack network list"
        }
        stage("Launch the instance") {
            echo "Getting image name to launch instance with:"
            try {
                CMD = "${SRCCMD} ; openstack image list | grep -v cirros|awk /active/'{print \$4}'"
                IMAGE_NAME = sh (
                script: CMD,
                returnStdout: true
                ).trim().tokenize().last()
            echo "IMAGE_NAME: ${IMAGE_NAME}"
            } catch (error) {
                echo "Could not get image name, failing build"    
                currentBuild.result = 'FAILURE'
            }
            try {
                // CMD = "${SRCCMD} ; ./tools/instance_launch.sh 1 ${IMAGE_NAME} | egrep -v 'project|user' | awk /id/'{print \$4}' | tail -n1"
                CMD = "${SRCCMD} ; ./tools/instance_launch.sh 1 ${IMAGE_NAME} | egrep -v 'project|user'"
                INSTANCE_OUTPUT = sh (
                    script: CMD,
                    returnStdout: true
                    ).trim()
                if ( INSTANCE_OUTPUT.contains("Error" ) ) {
                    echo "Error: ${INSTANCE_OUTPUT}"         
                    sh "openstack server show ${INSTANCE_OUTPUT.split(' ')[-1]}"
                    currentBuild.result = 'FAILURE'
                } else {
                    INSTANCE_OUTPUT.eachLine { line, count ->
                        if ( line.contains('id') ) {
                            INSTANCE_NAME = line.split(' ')[3]
                        }
                    }
                }
                echo "INSTANCE_NAME: ${INSTANCE_NAME}"
            } catch (error) {
                echo "Error launching instance: ${error}"
                currentBuild.result = 'ABORTED'
            } finally {
                if ( INSTANCE_NAME == "1" ) {
                    error "Invalid instance name, aborting"
                } 
            }
            try {
                CMD = "${SRCCMD} ; ./tools/float_all_new.sh"
                sh (
                    script: CMD,
                    returnStdout: true
                ).trim() 
            } catch (error) {
                echo "Error creating or assigning floating ip: ${error}"
            }
            try {
                CMD = "${SRCCMD} ; openstack server list |grep ${INSTANCE_NAME}| awk '{print \$9}'"
                FLOATING_IP = sh (
                    script: CMD,
                    returnStdout: true
                ).trim()
                echo "Floating IP: ${FLOATING_IP}."
            } catch (error) {
               echo "Error getting floating ip: ${FLOATING_IP}" 
            }
            /*waitUntil {
                try {
                    sh "ssh-keygen -f ~/.ssh/known_hosts -R ${FLOATING_IP}"
                    sh "ssh-keyscan -H ${FLOATING_IP} >> ~/.ssh/known_hosts"
                    return true
                } catch (error) {
                    echo "Error adding ssh fingerprint, trying again" 
                    sleep(30)
                    return false
                }
            }*/
            retries = 0
            waitUntil {
                CMD = "${SRCCMD} ; ssh -o StrictHostKeyChecking=no -i ~/testkey.pem -l ubuntu ${FLOATING_IP} uname -a"
                echo "UNAME_A cmd: ${CMD}"
                try {
                    UNAME_A = sh (
                        script: CMD,
                        returnStdout: true
                    ).trim()
                    return true
                } catch (error) {
                    retries++
                    if ( retries == 5 ) {
                        echo "Tried 5 times, aborting"
                        currentBuild.result = 'ABORTED'
                        error "Exceed maximum retries"
                    }
                    echo "Error getting uname from host: ${error}, retrying"
                    sleep(30)
                    return false
                }
            } 
            echo "Result of running uname -a on ${FLOATING_IP}: ${UNAME_A}"
            }
        }
    }
}

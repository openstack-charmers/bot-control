if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' || CLOUD_NAME=='amontons' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}

if ( ! params.OPENSTACK ) {
    error "This is not an openstack deployment"
}

if ( ! params.ZAZA) {
    error "This is not a zaza test
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

echo "Tempest tests selected: ${TEST_TYPE}"
// Simple test to launch a single openstack instance

SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"

TEST_CMD = "func"

def test_runner(TEST_CMD) {
    try {
        TEST_RUN = sh (
            script: "#!/bin/bash \nset -o pipefail ; tox -e ${TEST_CMD} | tee ${TEST_CMD}_${BUILD_ID}_output.log",
            returnStdout: true
        )
        sh "cat ${TEST_CMD}_${BUILD_ID}_output.log"
        archiveArtifacts artifacts: "*output.log"
    } catch (error) {
        echo "Error with test runner: ${error}"
        currentBuild.result = 'FAILURE'
    }
}

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        dir("${env.HOME}/tools/bundle_repo/") {
        stage('Configure tempest') {
                try {
                    JUJU_SWITCH = sh (
                        script: "juju switch ${ARCH}-mosci-${CLOUD_NAME}:${ARCH}-mosci-${CLOUD_NAME}",
                        returnStdout: true
                    )
                } catch (error) {
                    echo "Couldn't juju switch: ${error}"
                }
                echo "SLAVE_NODE_NAME: ${SLAVE_NODE_NAME}"
                echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
                    try {
                    ENVARS = sh (
                        script: "tools/configure_tempest.sh",
                        returnStdout: true
                    ) 
                    } catch (error) {
                        echo "error configuring tempest ${error}"
                    }
                }
        stage("Run tempest test") {
            env.HTTP_PROXY="http://squid.internal:3128"
            env.HTTPS_PROXY="http://squid.internal:3128"
            if ( params.ARCH == "s390x" ) {
                tempestdir = "${env.HOME}/tools/zopenstack/tools/2-configure/tempest"
            } else {
                tempestdir = "${env.HOME}/tools/openstack-charm-testing/tempest"
            }
            dir(tempestdir) {
            if ( params.TEST_TYPE == "all-plugin" ) {
                for ( i = 0 ; i < params.TEST_CLASSES.size() ; i++ ) {
                    cmd = "all-plugin -- ${params.TEST_CLASSES[i]}"
                    echo "CMD: ${CMD}"
                    test_runner(cmd)
                }
            } else {
                echo "TEST_TYPE: ${TEST_TYPE}"
                test_runner("${TEST_TYPE}")
                }
            }
        }
        stage("Clean up") {
            echo "some cleanup may be required between tests"
            }
        }
    }
}

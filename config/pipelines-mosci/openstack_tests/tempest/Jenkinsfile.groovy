if ( ! params.OPENSTACK ) {
    error "This is not an openstack deployment"
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

echo "Tempest tests selected: ${TEST_TYPE}"
// Simple test to launch a single openstack instance

SRCCMD = "#!/bin/bash \nsource ./novarcv3_project > /dev/null 2>&1" 

def test_runner(TEST_CMD) {
    try {
        TEST_RUN = sh (
            script: "tox -e ${TEST_CMD}",
            returnStdout: true
        )
    } catch (error) {
        echo "Error with test runner: ${error}"
    }
}

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        /*stage('Get OS Env vars') {
                echo "SLAVE_NODE_NAME: ${SLAVE_NODE_NAME}"
                echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
                    try {
                    ENVARS = sh (
                        script: "#!/bin/bash \nsource ./novarcv3_project ; env|grep OS_",
                        returnStdout: true
                    ) 
                    } catch (error) {
                        echo "Error getting env vars: ${error}"
                    }
                }*/
        stage("Run tempest test") {
            env.HTTP_PROXY="http://squid.internal:3128"
            env.HTTPS_PROXY="http://squid.internal:3128"
            dir("${env.HOME}/tools/openstack-charm-testing/tempest") {
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
        stage("Get logs") {
            echo "get some logs"
        }
        stage("Clean up") {
            echo "some cleanup may be required between tests"
            }
        }
    }
}

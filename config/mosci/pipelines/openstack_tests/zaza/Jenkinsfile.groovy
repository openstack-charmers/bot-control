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

bundle_repo = params.BUNDLE_REPO.split(',')[0]
bundle_repodir = params.BUNDLE_REPO.split(,)[1]


def test_runner(TEST_CMD) {
    node(params.SLAVE_NODE_NAME) {
        ws(params.WORKSPACE) {
            dir("${env.HOME}/tools/bundle_repo/${bundle_repodir}") {
                try {
                    TEST_RUN = sh (
                        script: "#!/bin/bash \nset -o pipefail ; functest-test --model ${params.MODEL_NAME} | tee ${TEST_CMD}_${BUILD_ID}_output.log",
                        returnStdout: true
                    )
                    sh "cat ${TEST_CMD}_${BUILD_ID}_output.log"
                    archiveArtifacts artifacts: "*output.log"
                } catch (error) {
                    echo "Error with test runner: ${error}"
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}

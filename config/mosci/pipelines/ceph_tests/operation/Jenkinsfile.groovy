if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

// Simple test to check ceph read / write

if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' || CLOUD_NAME=='virtual' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}

SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"

TEST_CMD = "sudo ceph osd pool create rbd 128; \
echo 123456789 > /tmp/input.txt; \
rados put -p rbd test_input /tmp/input.txt; \
rados get -p rbd test_input /dev/stdout"

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        dir("${env.HOME}/tools/openstack-charm-testing/") {
            stage('Get first ceph-mon node ID') {
                try {
                    MON_NAME = sh (
                        script: "juju status ceph-mon|grep ceph-mon/|head -n1|awk '{print \$1}'|tr -d '*'",
                        returnStdout: true
                    ).trim()
                echo "Checking ${MON_NAME}"
                } catch (error) {
                    echo "Couldn't get first ceph-mon unit name: ${error}"
                }
            }
            stage("Test RBD read and write") {
                echo "Test RBD read and write"
                try {
                    TEST_INPUT = sh (
                        script: "juju run --unit ${MON_NAME} \"${TEST_CMD}\"",
                        returnStdout: true
                    )
                echo "TEST_INPUT: ${TEST_INPUT}"
                if ( TEST_INPUT.contains("123456789") ) {
                    echo "RBD get is correct"
                } else {
                    echo "RBD get is incorrect"
                    try {
                        sh "mkdir -p crashdumps ; /snap/bin/juju-crashdump -o crashdumps/ceph-operation-${BUILD_ID}.tar.xz"
                        archiveArtifacts 'crashdumps/*'
                    } catch(error) {
                        echo "Error getting crashdump"
                    }
                    currentBuild.result = 'FAILURE'
                }
                } catch (error) {
                    echo "ceph rbd failure: ${error}"
                    try {
                        sh "mkdir -p crashdumps ; /snap/bin/juju-crashdump -o crashdumps/ceph-operation-${BUILD_ID}.tar.xz"
                        archiveArtifacts 'crashdumps/*'
                    } catch(errorInside) {
                        echo "Error getting crashdump"
                    }
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}

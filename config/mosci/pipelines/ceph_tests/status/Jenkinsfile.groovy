if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

// Simple test to check ceph status

if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}

SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"

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
            stage("Get ceph status") {
                echo "Get ceph status"
                try {
                    CEPH_STATUS = sh (
                        script: "juju run --unit ${MON_NAME} \"sudo ceph -s\"",
                        returnStdout: true
                    )
                echo "CEPH_STATUS: ${CEPH_STATUS}"
                if ( CEPH_STATUS.contains("HEALTH_OK") ) {
                    echo "ceph status HEALTH_OK, SUCCESS"
                } else {
                    echo "ceph status HEALTH_WARN or error, FAILURE"
                    currentBuild.result = 'FAILURE'
                }
                } catch (error) {
                    echo "ceph status (ceph -s) failed: ${error}"
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}

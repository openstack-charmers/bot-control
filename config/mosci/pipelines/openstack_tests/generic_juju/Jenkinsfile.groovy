if ( ! params.OPENSTACK ) {
    error "This is not an openstack deployment"
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}
if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}

/* This test can utilises existing pipeline jobs to bootstrap on top of an openstack deployment,
   deploy a bundle, wait for that bundle to be ready, and then tear it down
   
   1. bootstrap a new juju controller on the slave: bootstrap phase currently doesnt handle openstack controller bootstrapping
   2. deploy a bundle to that controller: deploy phase
   3. wait for that deployment to complete successfully: deploy phase
   4. tear the controller down (not necessary, but probably a good idea in between tests) 

   To-do: It would be possible to run this whole job on a new slave to avoid cross-controller 
   issues, but for now speed is the priorty.

   Note: a new slave is required due to executor limits being 2 per slave.

*/

echo "BUNDLE_URL: ${BUNDLE_URL}"

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        stage("Sideload Test Prep: ${params.ARCH}") {
        // Bootstrap the environment from ${CLOUD_NAME}
            try {
                OVERCLOUD_NAME = sh (
                    script: "juju controllers | awk /mosci/'{print \$1}'",
                    returnStdout: true
                ).trim().replace('*','')
            } catch (error) {
                echo "Error getting overcloud name: ${error}"
            }
            dir("${env.HOME}/tools/openstack-charm-testing/") {
                try {
                    JUJU_SWITCH = sh (
                        script: "juju switch ${ARCH}-mosci-${CLOUD_NAME}:${ARCH}-mosci-${CLOUD_NAME}",
                        returnStdout: true
                    )
                } catch (error) {
                    echo "Couldn't juju switch: ${error}"
                }
                SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"
                try {
                    KEYSTONE_AUTH_URL = sh (
                        script: "${SRCCMD} ; env|grep OS_AUTH_URL|awk -F'=' '{print \$2}'",
                        returnStdout: true
                    )
                echo "KEYSTONE_AUTH_URL: ${KEYSTONE_AUTH_URL}"
                } catch (error) {
                    echo "Couldn't get KEYSTONE_AUTH_URL: ${error}"
                }
            }
            echo "SLAVE_NODE_NAME: ${SLAVE_NODE_NAME}"
            echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
            echo "Bootstrapping controller ${CONTROLLER_NAME} on ${OVERCLOUD_NAME}"
            echo "${OVERCLOUD_NAME} is built on metal from ${CLOUD_NAME}"
            echo "Jenkins slave name is ${SLAVE_NODE_NAME}, ip ${OPENSTACK_PUBLIC_IP}"
        }
    }
}
node('master') {
    stage("Bootstrap on overcloud: ${params.ARCH}") {
            bootstrap_job = build job: '2. Full Cloud - Bootstrap', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                            [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                            [$class: 'StringParameterValue', name: 'MODEL_NAME', value: params.MODEL_NAME],
                            [$class: 'StringParameterValue', name: 'OVERCLOUD_NAME', value: OVERCLOUD_NAME],
                            [$class: 'StringParameterValue', name: 'CONTROLLER_NAME', value: params.CONTROLLER_NAME],
                            [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: ""],
                            [$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: ""],
                            [$class: 'StringParameterValue', name: 'KEYSTONE_AUTH_URL', value: KEYSTONE_AUTH_URL],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                            [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                            [$class: 'BooleanParameterValue', name: 'OVERCLOUD_DEPLOY', value: Boolean.valueOf(OVERCLOUD_DEPLOY)]]
        }
        stage("Deploy on overcloud: ${params.ARCH}") {
            echo 'Deploy'
            deploy_job = build job: '3. Full Cloud - Deploy', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CONTROLLER_NAME],
                         [$class: 'StringParameterValue', name: 'CONTROLLER_NAME', value: params.CONTROLLER_NAME],
                         [$class: 'StringParameterValue', name: 'MODEL_NAME', value: params.MODEL_NAME],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: params.BUNDLE_URL],
                         [$class: 'BooleanParameterValue', name: 'OVERCLOUD_DEPLOY', value: Boolean.valueOf(OVERCLOUD_DEPLOY)]]
                //sh 'ls -lart'
                //sh 'cd runners/manual-examples ; ./openstack-base-xenial-ocata-arm64-manual.sh'
        }
        stage("Teardown overcloud: ${params.ARCH}") {
            echo 'Teardown'
            deploy_job = build job: '6. Full Cloud - Teardown', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: "${params.CLOUD_NAME}"],
                         [$class: 'StringParameterValue', name: 'ARCH', value: "${params.ARCH}"],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'MODEL_NAME', value: "${CONTROLLER_NAME}"],
                         [$class: 'BooleanParameterValue', name: 'RELEASE_MACHINES', value: false],
                         [$class: 'BooleanParameterValue', name: 'OFFLINE_SLAVE', value: false],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_SLAVE', value: false],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_CONTROLLER', value: true],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_MODEL', value: true],
                         [$class: 'BooleanParameterValue', name: 'OVERCLOUD_DEPLOY', value: true]]
        }
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}

/* This test can utilise existing pipeline jobs.

   1. bootstrap a new juju controller on a NEW slave: bootstrap phase currently doesnt handle openstack controller bootstrapping
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
        stage("Bootstrap: ${params.ARCH}") {
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
            SRCCMD = "#!/bin/bash \n. ./novarcv3_project > /dev/null 2>&1"
            try {
                KEYSTONE_AUTH_URL = sh (
                    script: "${SRCCMD} ; echo $OS_AUTH_URL",
                    returnStdout: true
                )
            } catch (error) {
                echo "Couldn't get KEYSTONE_AUTH_URL: ${error}"
                }
            }
            echo "Bootstrapping controller ${CONTROLLER_NAME} on ${OVERCLOUD_NAME}"
            echo "${OVERCLOUD_NAME} is built on metal from ${CLOUD_NAME}"
            echo "Jenkins slave name is ${SLAVE_NODE_NAME}, ip ${OPENSTACK_PUBLIC_IP}"
            bootstrap_job = build job: '2. Full Cloud - Bootstrap', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                            [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                            [$class: 'StringParameterValue', name: 'MODEL_NAME', value: params.MODEL_NAME],
                            [$class: 'StringParameterValue', name: 'OVERCLOUD_NAME', value: OVERCLOUD_NAME],
                            [$class: 'StringParameterValue', name: 'CONTROLLER_NAME', value: params.CONTROLLER_NAME],
                            [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                            [$class: 'StringParameterValue', name: 'KEYSTONE_AUTH_URL', value: KEYSTONE_AUTH_URL],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "mosci-sideload-magpie"],
                            [$class: 'BooleanParameterValue', name: 'OVERCLOUD_DEPLOY', value: Boolean.valueOf(OVERCLOUD_DEPLOY)]]
                            //[$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: "${params.MODEL_CONSTRAINTS}"],
                            //[$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: "${params.BOOTSTRAP_CONSTRAINTS}"]]
        }
        stage("Deploy: ${params.ARCH}") {
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
        /*stage("Teardown: ${params.ARCH}") {
            echo 'Teardown'
            deploy_job = build job: '6. Full Cloud - Teardown', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: "${params.CLOUD_NAME}"],
                         [$class: 'StringParameterValue', name: 'ARCH', value: "${params.ARCH}"],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: params.MODEL_CONSTRAINTS],
                         [$class: 'StringParameterValue', name: 'MAAS_OWNER', value: params.MAAS_OWNER],
                         [$class: 'StringParameterValue', name: 'S390X_NODES', value: params.S390X_NODES],
                         [$class: 'BooleanParameterValue', name: 'RELEASE_MACHINES', value: Boolean.valueOf(RELEASE_MACHINES)],
                         [$class: 'BooleanParameterValue', name: 'FORCE_RELEASE', value: Boolean.valueOf(FORCE_RELEASE)],
                         [$class: 'BooleanParameterValue', name: 'OFFLINE_SLAVE', value: Boolean.valueOf(OFFLINE_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_SLAVE', value: Boolean.valueOf(DESTROY_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_CONTROLLER', value: Boolean.valueOf(DESTROY_CONTROLLER)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_MODEL', value: Boolean.valueOf(DESTROY_MODEL)],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: "${params.BUNDLE_URL}"]]
        }*/
    }
}

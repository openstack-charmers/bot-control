node('master') {
    stage('Trigger 0. Full Cloud - Pipeline') {
        trigger_wrapper = build job: '0. Full Cloud - Pipeline', parameters: 
        [[$class: 'BooleanParameterValue', name: 'OPENSTACK', value: params.OPENSTACK],
        [$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
        [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
        [$class: 'StringParameterValue', name: 'DISPLAY_NAME', value: params.DISPLAY_NAME],
        [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: params.SLAVE_NODE_NAME],
        [$class: 'BooleanParameterValue', name: 'CLEANUP_ON_FAILURE', value: params.CLEANUP_ON_FAILURE],
        [$class: 'BooleanParameterValue', name: 'FORCE_NEW_CONTROLLER', value: params.FORCE_NEW_CONTROLLER],
        [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: params.BUNDLE_URL],
        [$class: 'StringParameterValue', name: 'BUNDLE_OVERLAYS', value: params.BUNDLE_OVERLAYS],
        [$class: 'StringParameterValue', name: 'BUNDLE_PASTE', value: params.BUNDLE_PASTE],
        [$class: 'StringParameterValue', name: 'SELECTED_TESTS', value: params.SELECTED_TESTS],
        [$class: 'StringParameterValue', name: 'PERSIST_SLAVE', value: params.PERSIST_SLAVE],
        [$class: 'StringParameterValue', name: 'OFFLINE_SLAVE', value: params.OFFLINE_SLAVE]]
    }
}

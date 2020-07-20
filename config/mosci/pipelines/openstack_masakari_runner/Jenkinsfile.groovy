// TESTS = "${params.SELECTED_TESTS},zaza test - openstack - zaza masakari"
FALSE = false
TRUE = true

node('master') {
    stage('Trigger 0. Full Cloud - Pipeline') {
        trigger_wrapper = build job: '0. Full Cloud - Pipeline', parameters: 
        [[$class: 'BooleanParameterValue', name: 'OPENSTACK', value: true],
        [$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
        [$class: 'StringParameterValue', name: 'PHASES', value: params.PHASES],
        [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
        [$class: 'StringParameterValue', name: 'DISPLAY_NAME', value: params.DISPLAY_NAME],
        [$class: 'BooleanParameterValue', name: 'CLEANUP_ON_FAILURE', value: false],
        [$class: 'BooleanParameterValue', name: 'FORCE_NEW_CONTROLLER', value: false],
        [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: params.BUNDLE_URL],
        [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
        [$class: 'StringParameterValue', name: 'BUNDLE_REPO_DIR', value: params.BUNDLE_REPO_DIR],
        [$class: 'StringParameterValue', name: 'BUNDLE_OVERLAYS', value: params.BUNDLE_OVERLAYS],
        [$class: 'StringParameterValue', name: 'OVERRIDE_BUNDLE_CONFIG', value: params.OVERRIDE_BUNDLE_CONFIG],
        [$class: 'StringParameterValue', name: 'SELECTED_TESTS', value: TESTS],
        [$class: 'BooleanParameterValue', name: 'PERSIST_SLAVE', value: params.PERSIST_SLAVE],
        [$class: 'BooleanParameterValue', name: 'OFFLINE_SLAVE', value: params.OFFLINE_SLAVE]]
    }
}

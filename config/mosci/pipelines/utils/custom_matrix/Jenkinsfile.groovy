// bundle-agnostic job runner. Runs jobs selected by params.SELECTED_JOBS with params.CUSTOM_PARAMS

node('master') {
    stage("Prepare dynamic job selection") {
    /* If i do it this way, all tests must have ALL params.
       i.e. if a new tests is added which requires new params,
       all previous jobs must have those params added...
    */
    echo "Cloud name set to ${params.CLOUD_NAME}"
    TESTS = params.SELECTED_TESTS.split(',')
    echo "Selected jobs: ${params.SELECTED_JOBS}"
    echo "Custom params: ${params.CUSTOM_PARAMS}"
    }
    for (int i = 0; i < TESTS.size(); i++ ) { 
        stage (TESTS[i]) {
            print TESTS[i]
            build job: TESTS[i], parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
        }
    }
}

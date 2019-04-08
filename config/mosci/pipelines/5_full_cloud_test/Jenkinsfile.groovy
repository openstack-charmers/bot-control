// bundle-agnostic test runner. Runs tests selected by params.SELECTED_TESTS

node('master') {
    stage("Prepare dynamic test selection") {
    /* If i do it this way, all tests must have ALL params.
       i.e. if a new tests is added which requires new params,
       all previous jobs must have those params added...
    */
    echo "Cloud name set to ${params.CLOUD_NAME}"
    TESTS = params.SELECTED_TESTS.split(',')
    echo "Selected tests: ${params.SELECTED_TESTS}"
    }
    for (int i = 0; i < TESTS.size(); i++ ) { 
        stage (TESTS[i]) {
            print TESTS[i]
            build job: TESTS[i], parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
        }
    }
}

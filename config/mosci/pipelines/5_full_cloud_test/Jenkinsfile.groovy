// bundle-agnostic test runner. Runs tests selected by params.SELECTED_TESTS

if ( params.CLOUD_NAME=='ruxton' || params.CLOUD_NAME=='icarus' ) {
    CLOUD_NAME="${params.CLOUD_NAME}-maas"
} else {
    CLOUD_NAME=params.CLOUD_NAME
}
CONTROLLER_NAME="${ARCH}-mosci-${CLOUD_NAME}"
if ( params.CLOUD_NAME == "lxd" ) {
    MODEL_NAME="${ARCH}-mosci-${CLOUD_NAME}-${LXD_IP}".replaceAll("[.]", "-")
} else {
    MODEL_NAME=CONTROLLER_NAME
}

echo "MODEL NAME: ${MODEL_NAME}"
echo "SELECTED_TESTS: ${params.SELECTED_TESTS}"

def zaza_tests_check() {
    // check if the tests.yaml in the bundle dir contains zaza config steps
    // if it does, we will configure the job with zaza and also attempt to run zaza tests
    // if it does not, we will do legacy configuration, and run selected tests
    echo "Checking ${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml for zaza configuration steps"
    // tests_yaml = readFile("${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml")
    TOXCMD = "tox -e venv -- /bin/true"
    ACTCMD = "#!/bin/bash \nsource \$(find . -name activate)"
    TESTCMD = "python3 -c \"import json, zaza.charm_lifecycle.utils; print(zaza.charm_lifecycle.utils.get_test_steps()['default_alias'])\""
    try {
        check_tests = sh (
            script: "${TOXCMD} ; ${ACTCMD} ; ${TESTCMD}",
            returnStdout: true
            )
        //echo ${check_tests}
        if ( check_tests.contains('[]')) {
            echo "no zaza tests found"
            return false
        } else {
            echo "found zaza tests"
            return true
        }
    } catch (error) {
        echo "Error ${error} checking for zaza, skipping"
    }
}

if ( params.SELECTED_TESTS ) {
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
                    [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
                    [$class: 'StringParameterValue', name: 'BUNDLE_REPO_DIR', value: params.BUNDLE_REPO_DIR],
                    [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                    [$class: 'StringParameterValue', name: 'MODEL_NAME', value: MODEL_NAME],
                    [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                    [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
            }
        }
    }
}
//} else {
//    echo "Selecteded tests NARP: ${params.SELECTED_TESTS}"
//}

node(SLAVE_NODE_NAME) {
    stage("Looking for zaza tests") {
        dir("${env.HOME}/bundle_repo/${BUNDLE_REPO_DIR}") {
            do_zaza_tests = zaza_tests_check()
        }
    }
}

node('master') {
    stage("Run zaza tests") {
        if ( do_zaza_tests == true ) {
            echo "Found zaza tests"
            build job: "automatic test - openstack - zaza", parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
                [$class: 'StringParameterValue', name: 'BUNDLE_REPO_DIR', value: params.BUNDLE_REPO_DIR],
                [$class: 'StringParameterValue', name: 'WORKSPACE', value: params.WORKSPACE],
                [$class: 'StringParameterValue', name: 'MODEL_NAME', value: MODEL_NAME],
                [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
        } else {
                echo "No zaza tests found."
        }
    }
}




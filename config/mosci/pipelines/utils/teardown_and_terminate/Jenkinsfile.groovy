import hudson.model.Computer.ListPossibleNames



if ( params.SLAVE_SELECT == "" || params.SLAVE_SELECT.contains("master") || params.SLAVE_SELECT.contains("error") ) {
    curentBuild.result = 'FAILURE'
    echo "SLAVE_SELECT invalid, failing"
    error "FAILURE"
}

node('master') {
    stage("Destroying controllers and slaves") {
    // setting cloud_name to "autodetect" means teardown will have to figure out the cloud_name once the job is running on the slave
    CLOUD_NAME = "autodetect"
    SLAVES = params.SLAVE_SELECT.split(',')
    echo "Selected slaves: ${params.SLAVE_SELECT}, size: ${SLAVES.size()}"
    }
    for (int i = 0; i < SLAVES.size(); i++ ) { 
        print "slave: ${SLAVES[i]}"
        stage ("Teardown: ${SLAVES[i]}") {
            ARCH = SLAVES[i].split('-')[0]
            print "ARCH: ${ARCH}"
            echo "ARCH: ${ARCH}, CLOUD_NAME: ${CLOUD_NAME}, SLAVE_NODE_NAME: ${SLAVES[i]}"
            // If this is an s390x deployment, we need to get the list of LPARs via ssh so we can populate S390X_NODES and reset their snapshots.
            // [$class: 'StringParameterValue', name: 'S390X_NODES', value: params.S390X_NODES],
            if ( ARCH == "s390x" ) {
                echo "Can't destroy s390x deployments yet, use full pipeline terminate"
                currentBuild.result = 'FAILURE'
                error "FAILURE"
            } else {
                RELEASE_MACHINES = false 
            }
            teardown_job = build job: '6. Full Cloud - Teardown', propagate: false, wait: false, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                [$class: 'StringParameterValue', name: 'ARCH', value: ARCH],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVES[i]],
                [$class: 'BooleanParameterValue', name: 'LXD', value: params.LXD],
                [$class: 'BooleanParameterValue', name: 'CRASHDUMP', value: false],
                [$class: 'BooleanParameterValue', name: 'RELEASE_MACHINES', value: Boolean.valueOf(RELEASE_MACHINES)],
                [$class: 'BooleanParameterValue', name: 'DESTROY_SLAVE', value: true],
                [$class: 'BooleanParameterValue', name: 'DESTROY_CONTROLLER', value: true]]
        }
    }
}

Jenkins.instance.getAllItems(Job).each{
  def jobBuilds=it.getBuilds()
	//for each of such jobs we can get all the builds (or you can limit the number at your convenience)
    jobBuilds.each { build ->
      def runningSince = groovy.time.TimeCategory.minus( new Date(), build.getTime() )
      def isrunning = build.isBuilding()
      def currentStatus = build.buildStatusSummary.message
      def cause = build.getCauses()[0] //we keep the first cause
      //This is a simple case where we want to get information on the cause if the build was 
      //triggered by an user
      def user = cause instanceof Cause.UserIdCause? cause.getUserId():""
      //This is an easy way to show the information on screen but can be changed at convenience
      if ( isrunning && build.toString().contains('Full Cloud - Pipeline') ) {
          // println "Build: ${build} | Since: ${runningSince} | Status: ${currentStatus} | Cause: ${cause} | User: ${user}" 
     
      // You can get all the information available for build parameters.
      def parameters = build.getAction(ParametersAction)?.parameters
        if ( parameters.value.contains(CLOUD_NAME) && parameters.value.contains(ARCH) ) {
        echo "Found CLOUD_NAME: ${CLOUD_NAME} with ${ARCH} already deployed."
        }
        //parameters.each {
        //if ( it.name != null ) {
        //    if ( it.name == 'CLOUD_NAME' || it.name == 'ARCH') {
        //    println "Type: ${it.class} Name: ${it.name}, Value: ${it.dump()}" 
        //    }
        //  }
		//}
      }
    }
}
node('master') {
        ws(workSpace) {
        environment {
            MODEL_CONSTRAINTS="${MODEL_CONSTRAINTS}"
            BOOTSTRAP_CONSTRAINTS="${BOOTSTRAP_CONSTRAINTS}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
        }
        stage("Preparation: ${params.ARCH}") {
            // Logic for differentiating between MAAS, s390x, or something else (probably lxd) 
            echo "Cloud name set to ${CLOUD_NAME}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            if ( PHASES.contains("Preparation") ) {
            prep_job = build job: '1. Full Cloud - Prepare', parameters: [[$class: 'StringParameterValue', name: 'CTI_GIT_REPO', value: "${params.CTI_GIT_REPO}"],
                       [$class: 'StringParameterValue', name: 'CTI_GIT_BRANCH', value: "${params.CTI_GIT_BRANCH}"],
                       [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                       [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                       [$class: 'StringParameterValue', name: 'ARCH', value: "${params.ARCH}"],
                       [$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                       [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: "${params.MODEL_CONSTRAINTS}"],
                       [$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: "${params.BOOTSTRAP_CONSTRAINTS}"]]
            // Enable / disable verbose logging
            /*if ("${VERBOSE_LOGS}") {
                for(String line : prep_job.getRawBuild().getLog(100)){
                        echo line
                }
            }*/
            }
        }
        stage("Bootstrap: ${params.ARCH}") {
        // Bootstrap the environment from ${CLOUD_NAME}
            echo "Bootstrapping $ARCH from ${CLOUD_NAME}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            if ( PHASES.contains("Bootstrap") ) {
            bootstrap_job = build job: '2. Full Cloud - Bootstrap', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                            [$class: 'BooleanParameterValue', name: 'FORCE_RELEASE', value: Boolean.valueOf(FORCE_RELEASE)],
                            [$class: 'BooleanParameterValue', name: 'FORCE_NEW_CONTROLLER', value: Boolean.valueOf(FORCE_NEW_CONTROLLER)],
                            [$class: 'BooleanParameterValue', name: 'PRE_RELEASE_MACHINES', value: Boolean.valueOf(PRE_RELEASE_MACHINES)],
                            [$class: 'BooleanParameterValue', name: 'BOOTSTRAP_ON_SLAVE', value: Boolean.valueOf(BOOTSTRAP_ON_SLAVE)],
                            [$class: 'StringParameterValue', name: 'ARCH', value: "${params.ARCH}"],
                            [$class: 'StringParameterValue', name: 'S390X_NODES', value: params.S390X_NODES],
                            [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                            [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: "${params.MODEL_CONSTRAINTS}"],
                            [$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: "${params.BOOTSTRAP_CONSTRAINTS}"]]
            /*if ("${VERBOSE_LOGS}") {
                for(String line : bootstrap_job.getRawBuild().getLog(100)){
                        echo line
                }
            }*/
            //sh 'cd examples ; ./controller-arm64.sh'
            }
        }
        stage("Deploy: ${params.ARCH}") {
            echo 'Deploy'
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            if ( PHASES.contains("Deploy") ) {
            deploy_job = build job: '3. Full Cloud - Deploy', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'NEUTRON_DATAPORT', value: NEUTRON_DATAPORT],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: "${params.BUNDLE_URL}"],
                         [$class: 'StringParameterValue', name: 'BUNDLE_PASTE', value: params.BUNDLE_PASTE],
                         [$class: 'StringParameterValue', name: 'OVERRIDE_BUNDLE_CONFIG', value: params.OVERRIDE_BUNDLE_CONFIG]]
                //sh 'ls -lart'
                //sh 'cd runners/manual-examples ; ./openstack-base-xenial-ocata-arm64-manual.sh'
            }
        }
        stage("Configure: ${params.ARCH}") {
            echo "Configure"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            if ( PHASES.contains("Configure") ) {
            configure_job = build job: '4. Full Cloud - Configure', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'KEYSTONE_API_VERSION', value: params.KEYSTONE_API_VERSION],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
            }
        }
        stage("Test: ${params.ARCH}") {
            echo 'Test Cloud'
            if ( PHASES.contains("Teardown") ) {
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            test_job = build job: '5. Full Cloud - Test', parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'SELECTED_TESTS', value: params.SELECTED_TESTS],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
            }
        }
        stage("Teardown: ${params.ARCH}") {
            echo 'Teardown'
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            if ( PHASES.contains("Teardown") ) {
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
            }
        }
    }
}

if (params.SLAVE_NODE_NAME) {
    specific_slave=params.SLAVE_NODE_NAME
}
else if (params.PERSIST_SLAVE) {
    specific_slave="${params.SLAVE_LABEL}-${ARCH}-persist"
} 
else {
    specific_slave="${params.SLAVE_LABEL}-${ARCH}"
}

if ( params.CLOUD_NAME.contains("390")) {
        S390X=true
}  else { S390X=false }

workSpace="full_pipeline-${env.BUILD_ID}"

echo "Slave selection logic has selected ${specific_slave}"

// We need a random wait here, because this next bit can be racy:
// Two jobs can run on this node, while one changes the label to 'locked'
// The second is already running there while the label is changed - and attempts to change locked to ... locked

// echo "Sleeping for a random amount of time, between 0 and 60 seconds..."
// sleep((long)(Math.random() * 120));

// Make sure arch is s390x if cloud is s390x and vice versa

echo "PHASES: ${PHASES}"

if ( params.CLEANUP_ON_FAILURE ) { 
    prop = false
} else {
    prop = true
}

if ( params.OS_RELEASE_NAME == "" ) {
    release = ""
    releases = ["stable", "icehouse", "juno", "kilo", "librety", "mitaka", "newton", "ocata", "pike", "queens", "rocky", "stein", "jewel", "luminous", "mimic"]
    echo "Trying to get release from bundle url..."
    for ( a in releases ) {
        if ( params.BUNDLE_URL.contains(a) ) {
            release = a
        }
    }
    try {
        if ( release == "" ) {
            release = "unknown"
        }
    } catch (error) {    
        release = "unknown"
        }
    echo "Release: ${release}"
} else { release = params.OS_RELEASE_NAME }

if ( params.DISTRO_NAME == "" ) {
    distro = ""
    distros = ["stable", "trusty", "xenial", "bionic", "cosmic"]
    echo "Trying to get distro from bundle url..."
    for ( a in distros ) {
        if ( params.BUNDLE_URL.contains(a) ) {
            distro = a
        }
    }
    try {
        if ( distro == "" ) {
            distro = "unknown"
        }
    } catch (error) {    
        distro = "unknown"
        }
    echo "Distro: ${distro}"
} else { distro = params.DISTRO_NAME }

if ( params.BUNDLE_TYPE == "" ) {
    bundletype = ""
    bundletypes = ["ceph-base", "openstack-base", "openstack-lxd", "openstack-refstack", "openstack-telemetry", "zopenstack"]
    echo "Trying to get bundle type (e.g. base, telemetry) from bundle url..."
    for ( a in bundletypes ) {
        if ( params.BUNDLE_URL.contains(a) ) {
            bundletype = a
        }
    }
    try {
        if ( bundletype == "" ) {
            bundletype = "unknown"
        }
    } catch (error) {    
        bundletype = "unknown"
        }
    echo "Bundle Type: ${bundletype}"
} else { distro = params.BUNDLE_TYPE }

s390xcheck = ["${params.CLOUD_NAME}", "${params.ARCH}"]
if ( s390xcheck.any { it.contains("390") } ) {
        if ( ! s390xcheck.every { it.contains("390") } ) {
                echo "${ARCH} and ${CLOUD_NAME} is not a supported combination"
                currentBuild.result = 'FAILURE'
                return
        }
}

// Modify BOOTSTRAP_CONSTRAINTS and MODEL_CONSTRAINTS if s390x

if ( params.CLOUD_NAME.contains("390") ) {
        BOOTSTRAP_CONSTRAINTS=''
        MODEL_CONSTRAINTS="arch=s390x"
}

/* Throttle the job here.

        This job needs to check if other jobs of ARCH are running, and if so, wait until they aren't.
        Jenkins should handle this but the Throttle Builds plugin apparently doesn't work with pipelines
        properly.

        So, we can get all builds, and check if there is already a build with ARCH in it. If yes, wait. If no, build.

*/        
node('master') {
    stage("[ ${bundletype}: ${distro}-${release} on ${params.ARCH} @ ${CLOUD_NAME} ]") {
        echo "."
    }
}

def resourceCheck(arch, required) {
    waitUntil {
        TAGS = MODEL_CONSTRAINTS.minus("arch=" + params.ARCH + " ")
        TAGS = TAGS.minus("tags=")
        TAGS = TAGS.replace(" ", "")
        TAGS = TAGS.split(",")
        if ( params.CLOUD_NAME.contains("ruxton") ) {
                MAAS_API_KEY = params.RUXTON_API_KEY
        } else if ( params.CLOUD_NAME.contains("icarus") ) {
                MAAS_API_KEY = params.ICARUS_API_KEY
        }
        primary_tag = TAGS[0]
        additional_tags = TAGS.join(",")
        def maas_api_cmd = ""
        echo "Primary tag: ${primary_tag}, additional_tags: ${additional_tags}, arch: ${arch}"
        maas_api_cmd = maas_api_cmd + "-o ${params.MAAS_OWNER} -m ${CLOUD_NAME}-maas -k ${MAAS_API_KEY} --count --tags ${primary_tag} --arch ${arch}"
        if ( primary_tag != additional_tags ) {
            maas_api_cmd = maas_api_cmd + " --additional ${additional_tags} "
        }
        dir("${env.HOME}/tools/openstack-charm-testing/") {
            try {
            AVAILABLE_MACHINES = sh (
                script: "./bin/maas_actions.py ${maas_api_cmd}",
                returnStdout: true
            ).trim().toInteger()
            } catch (error) { echo "stuff: ${error}"}
            echo "Available machines: ${AVAILABLE_MACHINES}"
            echo "Required by bundle: ${required}"
            if ( AVAILABLE_MACHINES < required ) {
                echo "Not enough machines available for bundle, waiting before trying again"
                sleep(300)
                return false
            } else { 
                echo "Enough available machines, continuing"
                return true 
            }
        }
    }
}

try {
    node ('master') {
        ws("${params.WORKSPACE}") {
            stage("\t[ resource check ]"){
                if ( ! S390X && ! params.PRE_RELEASE_MACHINES && ! params.SKIP_RESOURCE_CHECK ) {
                    if (fileExists('bundle.yaml')) {
                        sh "rm bundle.yaml"
                    }
                    try {
                        sh "curl ${params.BUNDLE_URL} -o bundle.yaml"
                    } catch (error) {
                        echo "Full bundle paste:"
                        writeFile file: "bundle.yaml", text: params.BUNDLE_PASTE
                    }
                    BUNDLE_MACHINES = sh (
                        script: "python -c 'import yaml,sys;data = yaml.safe_load(sys.stdin);print(len(data[\"machines\"]))' < bundle.yaml",
                        returnStdout: true
                    ).trim().toInteger()
                    msg = ""
                    if ( ! params.BOOTSTRAP_CONSTRAINTS.contains("arch") && ! params.BOOTSTRAP_CONSTRAINTS.contains(params.ARCH) ) {
                        CONTROLLER_ARCH = ""
                        BUNDLE_MACHINES = BUNDLE_MACHINES + 1
                        msg = "including controller"
                    } else {
                        CONTROLLER_ARCH = BOOTSTRAP_CONSTRAINTS.split("arch=")[1].split(' ')[0]
                        msg = "excluding controller, which is an ${CONTROLLER_ARCH} instance"
                    }
                    echo "${BUNDLE_MACHINES} machines required by bundle.yaml ${msg}"
                    timeout(params.RESOURCE_CHECK_TIMEOUT.toInteger()) {
                        if ( CONTROLLER_ARCH != "" ) {
                            echo "Controller arch. ${CONTROLLER_ARCH} is different to deployment arch. ${params.ARCH}, checking MAAS for free controller machines..."
                            resourceCheck(CONTROLLER_ARCH, 1)
                        }
                        resourceCheck(params.ARCH, BUNDLE_MACHINES)    
                    }
                } else {
                    echo "Not trying to count machines: either s390x deploy or PRE_RELEASE_MACHINES = true" 
                }
            }
        }
    } 
} catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException error) {
    echo "Timed out waiting for machines" 
    currentBuild.result = 'FAILURE'
    error "FAILURE"
} catch (error) {
    echo "Problem checking number of machines, going blind: ${error}"
}

stage ("[ build slave ]") {
    waitUntil {
    node (specific_slave) { 
            echo "Picking ${NODE_NAME} for this job run"
            echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
            for ( node in jenkins.model.Jenkins.instance.nodes ) {
                    if (node.getNodeName().equals(NODE_NAME)) {
                            OLD_LABEL=node.getLabelString()
                            if ( OLD_LABEL == "locked" ) {
                                    return false
                            }
                            NEW_LABEL="locked"
                            SLAVE_NODE_NAME=node.getNodeName()
                            node.setLabelString(NEW_LABEL)
                            node.save()
                            echo "Changing node label from ${OLD_LABEL} to ${NEW_LABEL}"
                    }
            } 
        } 
    return true
    }
}

pipeline_state = ""
node(SLAVE_NODE_NAME) {
        ws(workSpace) {
        environment {
            MODEL_CONSTRAINTS="${MODEL_CONSTRAINTS}"
            BOOTSTRAP_CONSTRAINTS="${BOOTSTRAP_CONSTRAINTS}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
        }
        if ( PHASES.contains("Preparation") ) {
            stage("[ prepare ]") {
            // Logic for differentiating between MAAS, s390x, or something else (probably oolxd)
            echo "Cloud name set to ${CLOUD_NAME}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            prep_job = build job: '1. Full Cloud - Prepare', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CTI_GIT_REPO', value: "${params.CTI_GIT_REPO}"],
                       [$class: 'StringParameterValue', name: 'CTI_GIT_BRANCH', value: "${params.CTI_GIT_BRANCH}"],
                       [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                       [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                       [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                       [$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                       [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: "${params.MODEL_CONSTRAINTS}"],
                       [$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: "${params.BOOTSTRAP_CONSTRAINTS}"]]
            pipeline_state = pipeline_state + prep_job.result
            // Enable / disable verbose logging
            /*if ("${VERBOSE_LOGS}") {
                for(String line : prep_job.getRawBuild().getLog(100)){
                        echo line
                }
            }*/
            } 
        } else { echo "Skipping Preparation stage" } 
        if ( PHASES.contains("Bootstrap") && ! pipeline_state.contains("FAILURE")) {
            stage("[ bootstrap ]") {
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            echo "Bootstrapping $ARCH from ${CLOUD_NAME}"
            bootstrap_job = build job: '2. Full Cloud - Bootstrap', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: params.CLOUD_NAME],
                            [$class: 'BooleanParameterValue', name: 'FORCE_RELEASE', value: Boolean.valueOf(FORCE_RELEASE)],
                            [$class: 'BooleanParameterValue', name: 'FORCE_NEW_CONTROLLER', value: Boolean.valueOf(FORCE_NEW_CONTROLLER)],
                            [$class: 'BooleanParameterValue', name: 'PRE_RELEASE_MACHINES', value: Boolean.valueOf(PRE_RELEASE_MACHINES)],
                            [$class: 'BooleanParameterValue', name: 'BOOTSTRAP_ON_SLAVE', value: Boolean.valueOf(BOOTSTRAP_ON_SLAVE)],
                            [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                            [$class: 'StringParameterValue', name: 'S390X_NODES', value: params.S390X_NODES],
                            [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                            [$class: 'StringParameterValue', name: 'BOOTSTRAP_TIMEOUT', value: params.BOOTSTRAP_TIMEOUT],
                            [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: params.MODEL_CONSTRAINTS],
                            [$class: 'StringParameterValue', name: 'BOOTSTRAP_CONSTRAINTS', value: params.BOOTSTRAP_CONSTRAINTS],
                            [$class: 'StringParameterValue', name: 'OVERRIDE_MODEL_CONFIG', value: params.OVERRIDE_MODEL_CONFIG],
                            [$class: 'StringParameterValue', name: 'OVERRIDE_CONTROLLER_CONFIG', value: params.OVERRIDE_CONTROLLER_CONFIG]]
            pipeline_state = pipeline_state + bootstrap_job.result
            /*if ("${VERBOSE_LOGS}") {
                for(String line : bootstrap_job.getRawBuild().getLog(100)){
                        echo line
                }
            }*/
            //sh 'cd examples ; ./controller-arm64.sh'
            }
        } else { echo "Skipping Bootstrap stage" } 
        if ( PHASES.contains("Deploy") && ! pipeline_state.contains("FAILURE")) {
            stage("[ deploy ]") {
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            echo 'Deploying Bundle'
            deploy_job = build job: '3. Full Cloud - Deploy', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                         [$class: 'BooleanParameterValue', name: 'MANUAL_JOB', value: Boolean.valueOf(MANUAL_JOB)],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'NEUTRON_DATAPORT', value: NEUTRON_DATAPORT],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: "${params.BUNDLE_URL}"],
                         [$class: 'StringParameterValue', name: 'BUNDLE_PASTE', value: params.BUNDLE_PASTE],
                         [$class: 'StringParameterValue', name: 'DEPLOY_TIMEOUT', value: params.DEPLOY_TIMEOUT],
                         [$class: 'StringParameterValue', name: 'OVERRIDE_BUNDLE_CONFIG', value: params.OVERRIDE_BUNDLE_CONFIG]]
            pipeline_state = pipeline_state + deploy_job.result
                //sh 'ls -lart'
                //sh 'cd runners/manual-examples ; ./openstack-base-xenial-ocata-arm64-manual.sh'
            }
        } else { echo "Skipping Deployment stage" } 
        if ( PHASES.contains("Configure") && Boolean.valueOf(OPENSTACK) == true && ! pipeline_state.contains("FAILURE")) {
            stage("[ configure ]") {
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            echo "Configuring Openstack Cloud"
            configure_job = build job: '4. Full Cloud - Configure', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'KEYSTONE_API_VERSION', value: params.KEYSTONE_API_VERSION],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
            pipeline_state = pipeline_state + configure_job.result
            }
        } else { echo "Skipping Configuration stage" } 
        if ( PHASES.contains("Test") && ! pipeline_state.contains("FAILURE")) {
            // stage("[ test: ${SELECTED_TESTS.replaceAll("pipeline test - ", "")} ]") {
            stage("[ test: ${SELECTED_TESTS.replaceAll(~/pipeline test - *[^-]* - /, "")} ]") {
            echo 'Testing Cloud Functionality'
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            test_job = build job: '5. Full Cloud - Test', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: CLOUD_NAME],
                         [$class: 'BooleanParameterValue', name: 'OPENSTACK', value: Boolean.valueOf(OPENSTACK)],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'SELECTED_TESTS', value: params.SELECTED_TESTS],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: SLAVE_NODE_NAME],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH]]
            pipeline_state = pipeline_state + test_job.result
            }
        } else { echo "Skipping Test stage" } 
        if ( pipeline_state.contains("FAILURE") && params.CLEANUP_ON_FAILURE ) {
            CRASHDUMP = true
            stage("< pipeline failed at last stage")
        } else { 
            CRASHDUMP = false 
        }
        if ( PHASES.contains("Teardown") ) {
            stage("[ teardown ]") {
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            echo 'Tearing down deployment'
            teardown_job = build job: '6. Full Cloud - Teardown', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CLOUD_NAME', value: "${params.CLOUD_NAME}"],
                         [$class: 'StringParameterValue', name: 'ARCH', value: "${params.ARCH}"],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'MODEL_CONSTRAINTS', value: params.MODEL_CONSTRAINTS],
                         [$class: 'StringParameterValue', name: 'MAAS_OWNER', value: params.MAAS_OWNER],
                         [$class: 'StringParameterValue', name: 'S390X_NODES', value: params.S390X_NODES],
                         [$class: 'BooleanParameterValue', name: 'CRASHDUMP', value: Boolean.valueOf(CRASHDUMP)],
                         [$class: 'BooleanParameterValue', name: 'RELEASE_MACHINES', value: Boolean.valueOf(RELEASE_MACHINES)],
                         [$class: 'BooleanParameterValue', name: 'FORCE_RELEASE', value: Boolean.valueOf(FORCE_RELEASE)],
                         [$class: 'BooleanParameterValue', name: 'OFFLINE_SLAVE', value: Boolean.valueOf(OFFLINE_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_SLAVE', value: Boolean.valueOf(DESTROY_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_CONTROLLER', value: Boolean.valueOf(DESTROY_CONTROLLER)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_MODEL', value: Boolean.valueOf(DESTROY_MODEL)],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: "${params.BUNDLE_URL}"]]
            }
        } else { 
        echo "Skipping Teardown stage" 
        }
        if ( pipeline_state.contains("FAILURE") && params.CLEANUP_ON_FAILURE ) {
            currentBuild.result = 'FAILURE'
            stage("___ pipeline failure ___") {
                failure_job = build job: 'Failure Job'
                echo "This stage only appears when a job has failed but is not red because CLEANUP_ON_FAILURE is true"
            }
        }
    }
}

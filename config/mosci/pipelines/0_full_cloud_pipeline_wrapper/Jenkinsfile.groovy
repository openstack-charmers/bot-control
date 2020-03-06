if (params.CLOUD_NAME.contains("serverstack")) {
    ARCH="overcloud"
}
if (params.LXD) {
    CLOUD_NAME="lxd"
    ARCH="lxd"
}
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

if ( params.CLOUD_NAME.contains("ruxton") ) {
        MAAS_API_KEY = params.RUXTON_API_KEY
} else if ( params.CLOUD_NAME.contains("icarus") ) {
        MAAS_API_KEY = params.ICARUS_API_KEY
} else if ( params.CLOUD_NAME.contains("amontons") ) {
        MAAS_API_KEY = params.AMONTONS_API_KEY
}

TAGS = MODEL_CONSTRAINTS.minus("arch=" + params.ARCH + " ").minus("tags=").replace(" ", "").split(",")
bootstrap_tag = BOOTSTRAP_CONSTRAINTS.split("tags=")[1].split(" ")[0]
primary_tag = TAGS[0]
additional_tags = TAGS.join(",")
maas_api_cmd = ""
echo "Primary tag: ${primary_tag}, additional_tags: ${additional_tags}, arch: ${ARCH}"



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

bundle_guessed = false
echo "BUNDLE_TYPE: ${BUNDLE_TYPE}"

if ( params.BUNDLE_URL == "" && params.BUNDLE_PASTE == "" && params.BUNDLE_REPO == "" && params.BUNDLE_FILE == "" ) {
    if ( ! params.RELEASE_NAME.contains("undefined") && ! params.DISTRO_NAME.contains("undefined") && ! params.BUNDLE_TYPE.contains("undefined") ) {
        echo "No bundle specified, pasted, uploaded - trying to construct URL from RELEASE_NAME, DISTRO_NAME and BUNDLE_TYPE"
        BUNDLE_STR = "${params.BUNDLE_TYPE}-${params.DISTRO_NAME}-${params.RELEASE_NAME}"
        BUNDLE_URL = "https://raw.githubusercontent.com/openstack-charmers/openstack-bundles/master/development/${BUNDLE_STR}/bundle.yaml" 
        bundle_guessed = true
        echo "Guessing URL: ${BUNDLE_URL}"
    }
}

if ( bundle_guessed != true && params.RELEASE_NAME.contains("undefined") ) {
    releases = ["stable", "icehouse", "juno", "kilo", "liberty", "mitaka", "newton", "ocata", "pike", "queens", "rocky", "stein", "train", "jewel", "luminous", "mimic", "nautilus"]
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
} else { release = params.RELEASE_NAME }
echo "Release: ${release}"

if ( bundle_guessed != true && params.DISTRO_NAME.contains("undefined") ) {
    distro = ""
    distros = ["stable", "trusty", "xenial", "bionic", "cosmic", "disco", "eoan"]
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
} else { distro = params.DISTRO_NAME }
echo "Distro: ${distro}"

if ( bundle_guessed != true && params.BUNDLE_TYPE.contains("undefined") ) {
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
} else { bundletype = params.BUNDLE_TYPE }
echo "Bundle Type: ${bundletype}"

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

if ( params.LXD ) {
    if ( params.NEUTRON_DATAPORT == "" ) {
        echo "NEUTRON_DATAPORT must be set for LXD deployment - defaulting to br-ex:eth1"
        NEUTRON_DATAPORT = "br-ex:eth1"  
    }
}

/* Throttle the job here.

        This job needs to check if other jobs of ARCH are running, and if so, wait until they aren't.
        Jenkins should handle this but the Throttle Builds plugin apparently doesn't work with pipelines
        properly.

        So, we can get all builds, and check if there is already a build with ARCH in it. If yes, wait. If no, build.

*/        
if ( params.DISPLAY_NAME == "" ) {
    DISPLAY_NAME = "[ ${bundletype}: ${distro}-${release} on ${params.ARCH} @ ${CLOUD_NAME} ]"
} else {
    DISPLAY_NAME = "[ ${params.DISPLAY_NAME} - ${bundletype}: ${distro}-${release} on ${params.ARCH} @ ${CLOUD_NAME} ]"
}
node ('master') {
    stage("${DISPLAY_NAME}") {
        configFileProvider(
            [configFile(fileId: '9e04159f-e485-4c54-9eff-806efa36e1ee', targetLocation: '~/tools/')]
            ) { echo "." }
    }
}

def resourceCheck(arch, required, bootstrap) {
    waitUntil {
        /* TAGS = MODEL_CONSTRAINTS.minus("arch=" + params.ARCH + " ")
        TAGS = TAGS.minus("tags=").replace(" ", "").split(",")
        TAGS = TAGS.replace(" ", "")
        TAGS = TAGS.split(",")
        additional_tags = TAGS.join(",")
        echo "Primary tag: ${primary_tag}, additional_tags: ${additional_tags}, arch: ${arch}" */
        if ( bootstrap ) { 
        check_tag = bootstrap_tag 
        } else { 
        check_tag = primary_tag
        }
        maas_api_cmd = ""
        maas_api_cmd = maas_api_cmd + " -o ${params.MAAS_OWNER} -m ${CLOUD_NAME}-maas -k ${MAAS_API_KEY} --count --tags ${check_tag} --arch ${arch}"
        /*if ( primary_tag != additional_tags ) {
            maas_api_cmd = maas_api_cmd + " --additional ${additional_tags} "
        }*/
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
                        sh "curl ${BUNDLE_URL} -o bundle.yaml"
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
                    if ( ! params.LXD ) {
                        timeout(params.RESOURCE_CHECK_TIMEOUT.toInteger()) {
                            if ( CONTROLLER_ARCH != "" ) {
                                echo "Controller arch. ${CONTROLLER_ARCH} is different to deployment arch. ${params.ARCH}, checking MAAS for free controller machines..."
                                resourceCheck(CONTROLLER_ARCH, 1, true)
                            }
                            resourceCheck(params.ARCH, BUNDLE_MACHINES, false)    
                        }
                    } else {
                        echo "This is an LXD deployment, so we only need one machine of ${params.ARCH}"
                        resourceCheck(params.ARCH, 1, false)
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



def getSystemIP(MAAS_ID) {
    dir("${env.HOME}/tools/openstack-charm-testing/") {
        maas_api_cmd =  " -o ${params.MAAS_OWNER} -m ${CLOUD_NAME}-maas -k ${MAAS_API_KEY} --interfaces ${MAAS_ID} --getips"
        try {
            SYSTEM_IP = sh (
                script: "./bin/maas_actions.py ${maas_api_cmd}|grep -vi no_ip_assigned|head -n1",
                returnStdout: true
            ).trim().tokenize().last()
            echo "Got first IP: ${SYSTEM_IP}"
            echo "Somehow set OPENSTACK_PUBLIC_IP to this IP in all jobs - I guess I can pass it as a param."
            return SYSTEM_IP
        } catch (error) {
            echo "Couldn't get IP for ${MAAS_ID}: ${error}"
            currentBuild.result = 'FAILURE' 
            error "FAILING"
        }
    }
}

def initLXDHost(IP_ADDRESS) {
    // wait for host to come up (deployment to be complete and SSH ready)
    sh "ssh ubuntu@${IP_ADDRESS}"
    /* here we should ssh to the host, create a jenkins user, configure it. basically run the cloud-init script from managed files
    at he end, lets create a 'initialised' file, so we know if this has already been done, and if we need to re-do it
    */
}

stage ("[ build slave ]") {
    /* node ('master') {
        lxdonline = false
        // if params.LXD here to check if there is already a slave with this label - but actually it doesnt matter if there is, 
        // because if there is it will continue. if there is no slave with this label... 
        if ( params.LXD ) {
            echo "params.LXD is true, checking lxd-${ARCH}"
            for ( node in jenkins.model.Jenkins.instance.nodes ) {
                if ( node.getNodeName().contains("lxd-${ARCH}")) {
                    if ( node.getChannel() != null ) {
                        echo "node ${node.getNodeName()} is online, continuing"
                        specific_slave = node.getNodeName()
                        lxdonline = true
                        return true
                    }
                }
            }
            if ( lxdonline != true ) {
                echo "node lxd-${ARCH} is not online - provisioning one from MAAS" 
                maas_api_cmd = " -o ${params.MAAS_OWNER} -m ${CLOUD_NAME}-maas -k ${MAAS_API_KEY} --count --tags ${primary_tag} --arch ${arch} --output"
                /*if ( primary_tag != additional_tags ) {
                    maas_api_cmd = maas_api_cmd + " --additional ${additional_tags} "
                }*/
                /*dir("${env.HOME}/tools/openstack-charm-testing/") {
                     timeout(params.RESOURCE_CHECK_TIMEOUT.toInteger()) {
                        waitUntil {
                            try {
                                MAAS_SYSID = sh (
                                    script: "./bin/maas_actions.py ${maas_api_cmd}",
                                    returnStdout: true
                                ).trim().tokenize().last()
                                echo "MAAS_SYSID: ${MAAS_SYSID}"
                                if ( MAAS_SYSID == "0" ) {
                                    echo "No machines available, wait for 1 minute before retrying"
                                    sleep(60)
                                    return false
                                }
                                return true
                            } catch (error) {
                                echo "Error picking LXD host to provision with maas: ${error}"
                                currentBuild.result = 'FAILURE' 
                                error "FAILING"
                            }
                        } 
                    }
                    maas_api_cmd = " --deploy -o ${params.MAAS_OWNER} -k ${MAAS_API_KEY} -m ${CLOUD_NAME}-maas --tags ${primary_tag} --system_id ${MAAS_SYSID} "
                    try {
                        MAAS_DEPLOY = sh (
                            script: "./bin/maas_actions.py ${maas_api_cmd}",
                            returnStdout: true
                        ).trim()
                        echo "Deployed ${MAAS_SYSID}: ${MAAS_DEPLOY}"
                        sleep(30)
                        initLXDHost(getSystemIP(MAAS_SYSID))
                    } catch (error) {
                        echo "Error deploying machine ${MAAS_SYSID}: ${error}"
                        currentBuild.result = 'FAILURE'
                        error "FAILING"
                    }
                } 
            } 
        } 
    }*/
    waitUntil {
    node (specific_slave) { 
            echo "Picking ${NODE_NAME} for this job run"
            try {
                if ( OPENSTACK_PUBLIC_IP == "" ) {
                    OPENSTACK_PUBLIC_IP = "unknown"     
                }
            } catch (error) {
               OPENSTACK_PUBLIC_IP = "lxd" 
            }            
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
            // Logic for differentiating between MAAS, s390x, or something else (probably lxd)
            echo "Cloud name set to ${CLOUD_NAME}"
            SLAVE_NODE_NAME="${env.NODE_NAME}"
            prep_job = build job: '1. Full Cloud - Prepare', propagate: prop, parameters: [[$class: 'StringParameterValue', name: 'CTI_GIT_REPO', value: "${params.CTI_GIT_REPO}"],
                       [$class: 'BooleanParameterValue', name: 'LXD', value: params.LXD],
                       [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
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
                            [$class: 'BooleanParameterValue', name: 'LXD', value: params.LXD],
                            [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
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
                         [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                         [$class: 'StringParameterValue', name: 'WORKSPACE', value: workSpace],
                         [$class: 'StringParameterValue', name: 'ARCH', value: params.ARCH],
                         [$class: 'StringParameterValue', name: 'POST_DEPLOY_CMD', value: params.POST_DEPLOY_CMD],
                         [$class: 'StringParameterValue', name: 'SLAVE_NODE_NAME', value: "${SLAVE_NODE_NAME}"],
                         [$class: 'StringParameterValue', name: 'NEUTRON_DATAPORT', value: NEUTRON_DATAPORT],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: BUNDLE_URL],
                         [$class: 'StringParameterValue', name: 'BUNDLE_OVERLAYS', value: "${params.BUNDLE_OVERLAYS.replaceAll('\n', ',')}"],
                         [$class: 'StringParameterValue', name: 'BUNDLE_PASTE', value: params.BUNDLE_PASTE],
                         [$class: 'StringParameterValue', name: 'DEPLOY_TIMEOUT', value: params.DEPLOY_TIMEOUT],
                         [$class: 'StringParameterValue', name: 'MODIFY_BUNDLE', value: params.MODIFY_BUNDLE],
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
                         [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
                         [$class: 'BooleanParameterValue', name: 'ZAZA', value: Boolean.valueOf(ZAZA)],
                         [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
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
                         [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                         [$class: 'StringParameterValue', name: 'BUNDLE_REPO', value: params.BUNDLE_REPO],
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
                         [$class: 'StringParameterValue', name: 'LXD_IP', value: params.LXD_IP],
                         [$class: 'BooleanParameterValue', name: 'LXD', value: params.LXD],
                         [$class: 'BooleanParameterValue', name: 'CRASHDUMP', value: Boolean.valueOf(CRASHDUMP)],
                         [$class: 'BooleanParameterValue', name: 'RELEASE_MACHINES', value: Boolean.valueOf(RELEASE_MACHINES)],
                         [$class: 'BooleanParameterValue', name: 'FORCE_RELEASE', value: Boolean.valueOf(FORCE_RELEASE)],
                         [$class: 'BooleanParameterValue', name: 'OFFLINE_SLAVE', value: Boolean.valueOf(OFFLINE_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_SLAVE', value: Boolean.valueOf(DESTROY_SLAVE)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_CONTROLLER', value: Boolean.valueOf(DESTROY_CONTROLLER)],
                         [$class: 'BooleanParameterValue', name: 'DESTROY_MODEL', value: Boolean.valueOf(DESTROY_MODEL)],
                         [$class: 'StringParameterValue', name: 'BUNDLE_URL', value: BUNDLE_URL]]
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

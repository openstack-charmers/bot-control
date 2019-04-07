// This method collects a list of Node names from the current Jenkins instance
@NonCPS
def nodeNames() {
  return jenkins.model.Jenkins.instance.nodes.collect { node -> node.name }
}

def names = nodeNames()

if ( CLOUD_NAME=='ruxton' || CLOUD_NAME=='icarus' || CLOUD_NAME=='amontons' ) {
                CLOUD_NAME="${CLOUD_NAME}-maas"
}


if ( "${params.MODEL_CONSTRAINTS}".contains("arch=") ) {
        echo "Overriding ARCH param with model constraint specific architecture"
} 
else { 
        env.MODEL_CONSTRAINTS="arch=${params.ARCH} ${MODEL_CONSTRAINTS}"
}

echo "MODEL_CONSTRAINTS=${MODEL_CONSTRAINTS}"


if ( "${params.BOOTSTRAP_CONSTRAINTS}".contains("arch=") ) {
        echo "Overriding ARCH param with bootstrap constraint specific architecture"
} 
else { 
        env.BOOTSTRAP_CONSTRAINTS="arch=${params.ARCH} ${BOOTSTRAP_CONSTRAINTS}" 
}

if ( params.LXD == true ) {
        if ( params.LXD_IP == "" ) {
                echo "LXD IP Not set, failing build"
                currentBuild.result = 'FAILURE'
        }
        else {
                LXD_DEPLOY = true
        }
}
echo "BOOTSTRAP_CONSTRAINTS=${BOOTSTRAP_CONSTRAINTS}"

/* stage('Check Nodes for controllers and models') {
        echo "All node names: " + names
        for (int i=0; i<names.size(); ++i) {
            def nodeName = names[i];
            // node(nodeName) {
            //    steps {
            if (nodeName.contains("slave")) {
                echo 'Names ' + nodeName
                curNode = nodeName.replaceAll(/-([^-]*)$/, '/$1')
                node(nodeName) {
                    try {
                        sh 'juju run --unit ' + curNode + ' "su - jenkins -c \'juju controllers\' | egrep -A1 \'ERROR|Controller\'"'
                    } catch(all) {
                        echo 'no controller found on ' + curNode
                    }
                }
            }
            //        }
            //    }
            }
}*/

echo "Sticking job to ${params.SLAVE_NODE_NAME} in ${params.WORKSPACE}"

//following is needed for running on its own, but not yet functional

if ("${params.SLAVE_NODE_NAME}" == '') {
    SLAVE_NODE_NAME="${params.SLAVE_LABEL}-${ARCH}"
}
else {
    SLAVE_NODE_NAME="${params.SLAVE_NODE_NAME}"
}

node('master') {
        // make sure the tools are on the master
        dir("${env.HOME}/tools/openstack-charm-testing/") {
            try {
                git url: "${params.OCT_GIT_REPO}", branch: "${params.OCT_GIT_BRANCH}"
            } catch (error) {
                echo "git error: ${error} - may be safe to ignore if concurrent jobs are running this"
            }
        }
        dir("${env.HOME}/tools/juju-wait/") {
            try {
                git url: "https://git.launchpad.net/juju-wait" 
            } catch (error) {
                echo "git error: ${error} - may be safe to ignore if concurrent jobs are running this"
            }
        }
}

node("${SLAVE_NODE_NAME}") {
    echo "OPENSTACK_PUBLIC_IP = ${OPENSTACK_PUBLIC_IP}"
    ws("${params.WORKSPACE}") {
        stage('Prepare repos') {
            dir("${env.HOME}/tools/charm-test-infra/") {
                git url: "${params.CTI_GIT_REPO}", branch: "${params.CTI_GIT_BRANCH}"
            }
            dir("${env.HOME}/tools/openstack-charm-testing/") {
                git url: "${params.OCT_GIT_REPO}", branch: "${params.OCT_GIT_BRANCH}"
            }
            dir("${env.HOME}/tools/juju-wait/") {
                git url: "https://git.launchpad.net/juju-wait" 
            }
            dir("${env.HOME}/logs/juju/") {
                sh "touch .placeholder"
            }
            dir("${env.HOME}/logs/openstack/") {
                sh "touch .placeholder"
            }
            if ( CLOUD_NAME.contains("390") ) {
                dir("${env.HOME}/tools/zopenstack") {
                    git url: "https://github.com/ubuntu-openstack/zopenstack.git"
                }
            }
        }
        stage('Wait for juju to be installed') {
            timeout(30) {
                waitUntil {
                    try {
                        sh "ls -lart /usr/bin/juju"
                        sh "hash -r ; juju --version"
                        return true
                    } catch (error) {
                        echo "Error checking juju: ${error} - snap may still be installing"
                        sleep(60)
                        return false
                    }
                }
            }
        }
        if ( CLOUD_NAME.contains("maas") ) {
            stage('Check MAAS credentials') {
                dir("${env.HOME}/cloud-credentials/") {
                    echo "Using credentials file"
                    if ( fileExists('./credentials.yaml') ) {
                        if ( fileExists("${env.HOME}/tools/charm-test-infra/") ) {
                            sh "cp ./credentials.yaml ${env.HOME}/tools/charm-test-infra/juju-configs/" 
                        }
                    }
                }
                dir("${env.HOME}/tools/charm-test-infra") {
                    SHORT_NAME=CLOUD_NAME.split('-')[0]
                    try { CHECK_AUTH=sh (
                            script: "grep maas_oauth_${SHORT_NAME} juju-configs/credentials.yaml",
                            returnStdout: true
                            ).trim()
                    }
                    catch(all) {
                        CHECK_AUTH=''
                    }
                    echo "${CHECK_AUTH}"
                    if ( SHORT_NAME == "ruxton" ) {
                        echo "short name is ruxton"
                        MAAS_API_KEY = params.RUXTON_API_KEY
                    } else if ( SHORT_NAME == "icarus" ) {
                                echo "short name is icarus"
                                MAAS_API_KEY = params.ICARUS_API_KEY
                    } else if ( SHORT_NAME == "amontons" ) {
                                echo "short name is amontons"
                                MAAS_API_KEY = params.AMONTONS_API_KEY
                    }
                    if ("${CHECK_AUTH}" != '') {
                        sh "pwd"
                        if ( "${MAAS_API_KEY}" != '') {
                            echo "FOUND MAAS API KEY"
                        } else {
                            def get_maas_api_key = input(
                                id: 'get_maas_api_key', message: 'MAAS API KEY for ${CLOUD_NAME}', parameters: [
                                [$class: 'TextParameterDefinition', defaultValue: 'secret', description: 'API KEY', name: 'API_KEY']]
                            )
                            echo ("API KEY: " + get_maas_api_key)
                            echo "DID NOT FIND MAAS API KEY"
                            def MAAS_API_KEY="${get_maas_api_key}"
                        }
                    }
                    dir("${env.HOME}/tools/charm-test-infra") {
                        sh "sed -i 's/{{ maas_oauth_${SHORT_NAME} }}/${MAAS_API_KEY}/g' juju-configs/credentials.yaml"
                        sh "cat juju-configs/credentials.yaml"
                    }
                    dir("${env.HOME}/cloud-credentials/") {
                        sh "cp ${env.HOME}/tools/charm-test-infra/juju-configs/credentials.yaml ."
                    }
                }
            }
        }
        echo "checking state of LXD_DEPLOY: ${LXD_DEPLOY}"
        if ( LXD_DEPLOY == true ) {
            stage('Configure LXD remote') {
                dir("${env.HOME}/cloud-credentials/") {
                    if ( fileExists('./credentials.yaml') ) {
                        if ( fileExists("${env.HOME}/tools/charm-test-infra/") ) {
                            sh "cp ./credentials.yaml ${env.HOME}/tools/charm-test-infra/juju-configs/"
                        }
                    }
                }
                dir("${env.HOME}/tools/charm-test-infra") {
                    SHORT_NAME="lxd"
                    try { CHECK_AUTH=sh (
                            script: "grep ${SHORT_NAME} juju-configs/credentials.yaml",
                            returnStdout: true
                            ).trim()
                    }
                    catch(all) {
                        CHECK_AUTH=''
                    }
                    echo "${CHECK_AUTH}"
                    dir("${env.HOME}/tools/charm-test-infra") {
                        try {
                            sh "cp ${env.HOME}/cloud-credentials/clouds.yaml ./juju-configs/"
                        } catch(all) {
                            echo "clouds.yaml has not yet been altered" 
                        }
                        readContent = readFile 'juju-configs/clouds.yaml'
                        LXD_UNDER = LXD_IP.replaceAll("[.]", "-")
                        if ( ! readContent.contains("${LXD_UNDER}")) {
                            writeFile file: 'juju-configs/clouds.yaml', text: readContent + "\n  lxd-${LXD_UNDER}\n    type:lxd\n    auth-types: [interactive,certificate]\n    endpoint: https://${LXD_UNDER}:8443\n"
                        }
                        /*sh "sed -i 's/__ENDPOINT_LXD__/${params.LXD_IP}/g' juju-configs/clouds.yaml"
                        sh "cat juju-configs/clouds.yaml"*/
                    }
                    dir("${env.HOME}/cloud-credentials/") {
                        readContent = readFile 'clouds.yaml'
                        if ( ! readContent.contains("${LXD_UNDER}")) {
                            writeFile file: 'clouds.yaml', text: readContent + "\n  lxd-${LXD_UNDER}\n    type:lxd\n    auth-types: [interactive,certificate]\n    endpoint: https://${LXD_UNDER}:8443\n"
                        }
                        // sh "cp ${env.HOME}/tools/charm-test-infra/juju-configs/clouds.yaml ."
                    }
                }
            }
        }
    }
}

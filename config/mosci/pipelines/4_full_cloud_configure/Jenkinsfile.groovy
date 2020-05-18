if (params.SLAVE_NODE_NAME == '') {
    echo "SLAVE_NODE_NAME must be defined, cannot configure on new slave"
    currentBuild.result = 'FAILURE'
}
echo "KSV: ${KEYSTONE_API_VERSION}"
if ( params.KEYSTONE_API_VERSION == null ) {
KEYSTONE_API_VERSION = ""
} else {
    KEYSTONE_API_VERSION = params.KEYSTONE_API_VERSION
}

if ("${params.SLAVE_NODE_NAME}" == '') {
    SLAVE_NODE_NAME="${params.SLAVE_LABEL}-${ARCH}"
}
else {
    SLAVE_NODE_NAME="${params.SLAVE_NODE_NAME}"
}


if ( params.OVERCLOUD_DEPLOY == true ) {
    CONTROLLER_NAME=params.CONTROLLER_NAME
    MODEL_NAME=params.MODEL_NAME
} else {
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
}
if ( params.CLOUD_NAME.contains("390")) {
        S390X=true
}  else { S390X=false }

CONMOD = "${CONTROLLER_NAME}:${MODEL_NAME}"

SRCCMD = "#!/bin/bash \nsource rcs/openrc > /dev/null 2>&1"

/* This phase is configuring the deployed cloud to be accessible via openstack commands
        To do that, we need to run ./configure profile_name
        To decide which profile name we need:
        arch (amd64 = dellstack), ppc64el, arm64
        if its an s390x deployment, we will need the zopenstack repo (lets get that in prepare)
        if its a mixed deployment it'll need a special profile
        * we may have to find out whether its before or after queens so we can figure out what openstack client 
        commands to use.
        then we need to find out what the keystone api version is so we can append v3 to the end of the profile
        then we run ./configure that_profile
*/

def get_keystone_api_version() {
        try {
        KEYSTONE_MAJOR_VERSION = sh (
            script: "juju status keystone -m ${CONMOD}|grep -i version -A1|tail -n1|awk '{print \$2}'|cut -d'.' -f1",
            returnStdout: true
            )
        } catch (error) {
            echo "Couldn't get keystone major version ${error}"
            currentBuild.result = 'FAILURE'
        } finally {
            if ( KEYSTONE_MAJOR_VERSION.contains("error") ) {
                echo "Error found with 'juju status keystone':"
                sh "juju status keystone -m ${CONMOD}"
                currentBuild.result = 'FAILURE' 
                error "Couldn't get keystone major version"
            }
        echo "Keystone Major version: ${KEYSTONE_MAJOR_VERSION}"
        }
        try {
        KAV = sh (
            script: "juju config keystone preferred-api-version -m ${CONMOD}",
            returnStdout: true
            )
        if ( KAV != '' ) {
            echo "Keystone API version: ${KEYSTONE_API_VERSION}"
            }
        // Keystone API v2.0 was removed in Keystone version 13
        // shipped with OpenStack Queens
        } catch (error) {
            echo "Couldn't get keystone API version: ${error}"
        } 
        if ( KEYSTONE_MAJOR_VERSION.toInteger() >= 13 ) {
            echo "Setting keystone api version to 3"
            KAV = '3'
            } else if ( KAV == '' ) {
                echo "Making an assumption that keystone api version is 2"
                KAV = '2'
        } 
        return KAV 
}

def bundle_url_to_repo() {
    // Try to get the BUNDLE_REPO and see if there are any zaza configuration steps
    // This only currently works with the openstack-bundles repo
    // GUESS_REPO = params.BUNDLE_URL.split('/')[0..4].join('/').replace('raw.githubusercontent.com','github.com')
    // GUESS_REPO_DIR = params.BUNDLE_URL.split('/')[8..6].reverse().join('/').minus('bundle.yaml')
    try {
        sh "rm -rf ${env.HOME}/bundle_repo/"
    } catch (error) {
        echo "No bundle_repo dir to delete - first run"
    }
    try {
        // sh "git clone ${params.BUNDLE_REPO} ${env.HOME}/bundle_repo/"
        sh "cd ${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR} ; tox -e venv -- /bin/true"
        if ( zaza_config_check() ) {
            zaza_check = true
            return true
        } else {
            zaza_check = false
        }
    } catch (error) {
        echo "Could not clone bundle repo ${params.BUNDLE_REPO} - bad guess? ${error}"
        echo "This means no zaza config for this bundle"
        zaza_check = false
        return false
    }
}

def zaza_config_check() {
    // check if the tests.yaml in the bundle dir contains zaza config steps
    // if it does, we will configure the job with zaza and also attempt to run zaza tests
    // if it does not, we will do legacy configuration, and run selected tests
    echo "Checking ${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml for zaza configuration steps"
    tests_yaml = readFile("${env.HOME}/bundle_repo/${params.BUNDLE_REPO_DIR}/tests/tests.yaml")
    if ( tests_yaml.contains('configure: []') ) {
        echo "no zaza configuration steps found"
        return false
    } else {
        echo "found zaza configuration steps"
        return true
    }
}

node(params.SLAVE_NODE_NAME) {
    ws(params.WORKSPACE) {
        stage('Setup') {
            KEYSTONE_API_VERSION = ''
            if ( params.KEYSTONE_API_VERSION != '' || params.KEYSTONE_API_VERSION != null) {
                KEYSTONE_API_VERSION = params.KEYSTONE_API_VERSION
            } 
            echo "SLAVE_NODE_NAME: ${params.SLAVE_NODE_NAME}"
            echo "OPENSTACK_PUBLIC_IP = ${env.OPENSTACK_PUBLIC_IP}"
            echo "We can use this stage to set the following:"
            echo '$GATEWAY, $CIDR_EXT, $FIP_RANGE, $NAMESERVER, $CIDR_PRIV, $SWIFT_IP'
            echo "Will need some kind of tracking/ip allocation subroutines"
            echo "Will also need to source novarc / set relevant vars here"
            if ( params.ARCH == 's390x' ) {
                    profile_prefix = "s390x-multi-lpar"
            } else { profile_prefix = params.ARCH }
            env.KEYSTONE_API_VERSION = ""
            echo "K A V: ${KEYSTONE_API_VERSION}, ${params.KEYSTONE_API_VERSION}"
            if ( params.KEYSTONE_API_VERSION == '' || params.KEYSTONE_API_VERSION == null ) {
                    echo "You have not specified a keystone api version, interrogating:"
                    KEYSTONE_API_VERSION = get_keystone_api_version()
                    echo "Keystone API version is: ${KEYSTONE_API_VERSION}"
            }
            echo "Doing this one...."
            if ( KEYSTONE_API_VERSION == "3" ) {
                    profile_name = profile_prefix + "-v3"
            } else { profile_name = profile_prefix }
        }
        stage("Configure Cloud") {
            bundle_url_to_repo()
            if ( zaza_check == true ) {
                echo "Configuring with zaza"
                dir("${env.HOME}/tools/openstack-charm-testing/") {
                    BUNDLE_VARS = readFile("profiles/${profile_name}" )
                    BUNDLE_VARS.split("\n").each { line_a, count_a ->
                        if ( line_a.contains("export GATEWAY") ) {
                            BUNDLE_GATEWAY = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_GATEWAY}"
                        }
                        if ( line_a.contains("export CIDR_EXT") ) {
                            BUNDLE_CIDR_EXT = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_CIDR_EXT}"
                        }
                        if ( line_a.contains("export FIP_RANGE") ) {
                            BUNDLE_FIP_RANGE = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_FIP_RANGE}"
                        }
                        if ( line_a.contains("export NAMESERVER") ) {
                            BUNDLE_NAMESERVER = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_NAMESERVER}"
                        }
                        if ( line_a.contains("export CIDR_PRIV") ) {
                            BUNDLE_CIDR_PRIV = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_CIDR_PRIV}"
                        }
                        if ( line_a.contains("export SWIFT_IP") ) {
                            BUNDLE_SWIFT_IP = line_a.split('"')[-1]
                            echo "Gateway found and setting to ${BUNDLE_SWIFT_IP}"
                        } else { BUNDLE_SWIFT_IP = "" }
                    }
                }
                env.GATEWAY = BUNDLE_GATEWAY
                env.CIDR_EXT = BUNDLE_CIDR_EXT
                env.FIP_RANGE = BUNDLE_FIP_RANGE
                env.NAMESERVER = BUNDLE_NAMESERVER
                env.CIDR_PRIV =  BUNDLE_CIDR_PRIV
                env.SWIFT_IP = BUNDLE_SWIFT_IP
                env.default_gateway = BUNDLE_GATEWAY
                env.external_dns = BUNDLE_NAMESERVER
                env.external_net_cidr = BUNDLE_CIDR_EXT
                env.start_floating_ip = BUNDLE_FIP_RANGE.split(":")[0]
                env.end_floating_ip = BUNDLE_FIP_RANGE.split(":")[1]
                dir("${env.HOME}/bundle_repo/${BUNDLE_REPO_DIR}") {
                    sh "tox -e venv -- /bin/true"
                    ACTCMD = "#!/bin/bash \nsource \$(find . -name activate)"
                    sh "${ACTCMD} ; functest-configure --model ${MODEL_NAME}"
                }
            } else {
                echo "legacy configuration with openstack-charm-testing"
                if ( params.CLOUD_NAME.contains("390") ) {
                    directory = "${env.HOME}/tools/zopenstack/tools/2-configure/"
                } else {
                    directory = "${env.HOME}/tools/openstack-charm-testing/"
                }
                dir(directory) {
                    try {
                        echo "Attempting to configure cloud with ./configure ${profile_name}"
                        env.WGET_MODE="--quiet"
                        env.BARE_METAL="true"
                        sh "juju switch ${CONMOD}"
                        sh "./configure ${profile_name}"
                        return true
                    } catch (error) {
                        echo "Configure script run failed: ${error}, failing."
                        currentBuild.result = 'FAILURE'
                        error "Configure Failed."
                        // currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        stage("Configure Cloud DNS") { 
            if ( ! params.BUNDLE_REPO ) { 
                dir("${env.HOME}/tools/openstack-charm-testing/") {
                    CMD = "${SRCCMD} ; openstack subnet show private_subnet|grep dns_nameservers|awk '{print \$4}'"
                    try {
                        DNS_SERVER = sh (
                            script: CMD,
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "Error getting DNS server: ${error}"
                    }
                    // if ^ = '|', then don't try to set it
                    if ( DNS_SERVER.contains("|") ) {
                        DNS_CMD = "dns-servers"
                    } else {
                        DNS_CMD = "dns-servers=${DNS_SERVER}"
                    }
                    API_CMD = "juju config neutron-api enable-ml2-dns=true reverse-dns-lookup=true -m ${CONMOD}"
                    GW_CMD = "juju config neutron-gateway -m ${CONMOD} ${DNS_CMD} "
                    SUBNET_CMD = "${SRCCMD} ; openstack subnet set private_subnet --no-dns-nameservers"
                    echo "Setting ${API_CMD}, ${GW_CMD} and ${SUBNET_CMD}"
                    try {
                        sh (
                            script: API_CMD,
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "Error setting neutron-api dns config, ${error}"
                    }
                    try {
                        sh (
                            script: GW_CMD,
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "Error setting neutron-gateway dns config, ${error}"
                    }
                    try {
                        sh (
                            script: SUBNET_CMD,
                            returnStdout: true
                        )
                    } catch (error) {
                        echo "Error setting neutron-api dns config, ${error}"
                    }
                }
            }
        }
    }
}

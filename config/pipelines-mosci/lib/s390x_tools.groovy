def s390x_snapshot_create(snapshot_machines) {
        echo "Attempting to create an LVM snapshot..."
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        for (int i = 0; i < snapshot_machines.length(); i++ ) { 
                sh "scp ./bin/snap_root_util.py ubuntu@s4lp${snapshot_machines[i]}:/home/ubuntu/"
                waitUntil {
                    try {
                        create_cmd = sh (
                        script: "ssh -i ~/.ssh/id_rsa_mosci ubuntu@s4lp${snapshot_machines[i]} /home/ubuntu/snap_root_util.py --check",
                        returnStatus: true
                    )
                    if ( create_cmd == 0 ) {
                    echo "Snapshot already exists, no need to create"
                    return true
                    } else if ( create_cmd == 1 ) {
                        echo "Existing snapshot found, restore in progress"
                        sleep(60)
                        return false 
                    } else if ( create_cmd == 2 ) {
                        echo "No snapshot found, creating" 
                        try {
                            sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@s4lp${snapshot_machines[i]} /home/ubuntu/snap_root_util.py --create"
                            return true
                        } catch (inner_error) {
                               echo "Error with snap root creation, failing build: ${inner_error}"
                               currentBuild.result = 'FAILURE'
                               return true
                        }
                    }
                    echo "Unhandled error: ${create_cmd}"
                    return false
                    } catch (error) {
                        echo "Machine may not be reachable, trying again... ${create_cmd}"
                        sleep(120)
                        return false
                    }
                    }
                }
        }
}

def s390x_snapshot_reset(reset_machines) {
        echo "Attempting to restore an LVM snapshot. If we do not find one, we will create one."
        dir("${env.HOME}/tools/openstack-charm-testing/") {
        for (int i = 0; i < reset_machines.length(); i++ ) { 
            waitUntil {
                try {
                    sh "scp ./bin/snap_root_util.py ubuntu@s4lp${reset_machines[i]}:/home/ubuntu/"
                    def check_snapshot = sh (
                            script: "ssh -i ~/.ssh/id_rsa_mosci ubuntu@s4lp${reset_machines[i]} /home/ubuntu/snap_root_util.py --restore",
                            returnStatus: true
                    )
                    if ( check_snapshot == 0 ) {
                        echo "Restore OK, rebooting machine and creating a new snapshot"
                        sh "ssh -i ~/.ssh/id_rsa_mosci ubuntu@s4lp${reset_machines[i]} sudo shutdown -r 1"
                        sleep(120)
                        s390x_snapshot_create(reset_machines[i])
                        return true 
                    } else if ( check_snapshot == 1 ) {
                        echo "Restore in progress"
                        sleep(60)
                        return false
                    } else if ( check_snapshot == 2 ) {
                        echo "No snapshot found, creating one"
                        s390x_snapshot_create(reset_machines[i])
                        return true
                    }
                echo "Unhandled exception: ${check_snapshot}"
                return false
                } catch (error) {
                echo "SSH error, retrying"
                sleep(60)
                return false
                }
            }
        }
    }
}

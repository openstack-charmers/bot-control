Set Up Jenkins Job Builder in a Virtual Env
===========================================

#### Example: Basic usage
```
virtualenv .vjjb
source .vjjb/bin/activate
pip install -r jjb-requirements.txt

jenkins-jobs --help

usage: jenkins-jobs [-h] [--conf CONF] [-l LOG_LEVEL] [--ignore-cache]
                    [--flush-cache] [--version] [--allow-empty-variables]
                    [--user USER] [--password PASSWORD]
                    {update,test,delete,delete-all} ...

positional arguments:
  {update,test,delete,delete-all}
                        update, test or delete job
    delete-all          delete *ALL* jobs from Jenkins server, including those
                        not managed by Jenkins Job Builder.

optional arguments:
  -h, --help            show this help message and exit
  --conf CONF           configuration file
  -l LOG_LEVEL, --log_level LOG_LEVEL
                        log level (default: info)
  --ignore-cache        ignore the cache and update the jobs anyhow (that will
                        only flush the specified jobs cache)
  --flush-cache         flush all the cache entries before updating
  --version             show version
  --allow-empty-variables
                        Don't fail if any of the variables inside any string
                        are not defined, replace with empty string instead
  --user USER, -u USER  The Jenkins user to use for authentication. This
                        overrides the user specified in the configuration file
  --password PASSWORD, -p PASSWORD
                        Password or API token to use for authenticating
                        towards Jenkins. This overrides the password specified
                        in the configuration file.
```

#### To exit the virtual env:
```
deactivate
```

------------------------------------------------------------------------------

#### Example: Iterating on JJB Template Authoring

Use the test command to see what XML job config would be generated for Jenkins.

```jenkins-jobs test <foo>``` simply sends the generated XML to stdout.

```
tox -e jjb
. jjbrc
jenkins-jobs --version

jenkins-jobs test .  > temp.xml

INFO:root:Will use anonymous access to Jenkins if needed.
INFO:jenkins_jobs.builder:Number of jobs generated:  79
INFO:jenkins_jobs.builder:Job name:  charm_pusher_aodh_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_aodh_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_barbican-softhsm_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_barbican-softhsm_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_barbican_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_barbican_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceilometer-agent_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceilometer-agent_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceilometer_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceilometer_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-fs_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-fs_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-mon_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-mon_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-osd_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-osd_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-proxy_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-proxy_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-radosgw_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph-radosgw_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_ceph_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder-backup_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder-backup_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder-ceph_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder-ceph_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_cinder_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_designate-bind_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_designate-bind_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_designate_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_designate_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_glance_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_glance_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_hacluster_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_hacluster_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_heat_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_heat_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_keystone-ldap_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_keystone-ldap_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_keystone_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_keystone_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_lxd_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_lxd_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_manila-generic_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_manila-generic_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_manila_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_manila_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-api-odl_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-api-odl_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-api_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-api_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-gateway_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-gateway_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-openvswitch_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_neutron-openvswitch_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_noop_debug
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-cloud-controller_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-cloud-controller_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-compute-proxy_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-compute-proxy_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-compute_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_nova-compute_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_odl-controller_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_odl-controller_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_openstack-dashboard_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_openstack-dashboard_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_openvswitch-odl_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_openvswitch-odl_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_percona-cluster_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_percona-cluster_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_rabbitmq-server_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_rabbitmq-server_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_swift-proxy_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_swift-proxy_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_swift-storage_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_swift-storage_stable
INFO:jenkins_jobs.builder:Job name:  charm_pusher_tempest_master
INFO:jenkins_jobs.builder:Job name:  charm_pusher_tempest_stable
INFO:jenkins_jobs.builder:Cache saved

vi temp.xml  # To see it.
```

------------------------------------------------------------------------------

#### Render Jobs into Jenkins using JJB

Create the conf file.  Here is an example:

```
ubuntu@osci-bastion:~/bot-control/tools⟫ cat uosci-jjb.conf
[job_builder]
ignore_cache=True
allow_duplicates=False

[jenkins]
user=XXXXXX
password=XXXXXX
url=http://n.n.n.n:8080/
```

And go!  This will create or update the jobs in Jenkins (CAUTION).

```
jenkins-jobs --conf uosci-jjb.conf update .
```

#### References
 - https://docs.openstack.org/infra/jenkins-job-builder/index.html
 - https://docs.openstack.org/infra/jenkins-job-builder/execution.html

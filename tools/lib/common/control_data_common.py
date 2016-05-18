#!/usr/bin/python3
'''
Control data helpers
'''

# Charms which should use the source config option
#   Ripped and extended from charmhelpers
#   charmhelpers/contrib/openstack/amulet/deployment.py
CHARMS_USE_SOURCE = [
    'ceph',
    'ceph-osd',
    'ceph-radosgw',
    'ceph-mon',
    'mongodb',
    'mysql',
    'percona-cluster',
    'rabbitmq-server',
]

# Charms which use openstack-origin, ie. generally NOT subordinates
CHARMS_USE_ORIGIN = [
    'ceilometer',
    'ceilometer-agent'
    'cinder',
    'glance',
    'heat',
    'keystone',
    'neutron-api',
    'neutron-gateway',
    'nova-cloud-controller',
    'nova-compute',
    'openstack-dashboard',
    'swift-proxy',
    'swift-storage',
]

OVERRIDE_KEYS_MAP = {
    'source': CHARMS_USE_SOURCE,
    'openstack-origin': CHARMS_USE_ORIGIN
}

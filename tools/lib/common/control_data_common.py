# Copyright 2016 Canonical Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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

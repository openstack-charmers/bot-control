# Install MOJO in a Python Virtual Env for OpenStack Specs

This is an example of non-standard way to consume and use Mojo.  Some helper code in the specs still needs to be updated for Juju 2.x, so this describes usage with Juju 1.25 Stable.


```
# Clone the tool to a tools directory
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
cd /home/ubuntu/tools/bot-control/tools

# Prep host
sudo add-apt-repository ppa:juju/1.25 -y
sudo apt-get update
sudo apt-get install juju-core git bzr python-pip tox libffi-dev -y

# Build venv
cd mojo-openstack
tox

# Source venv and use it
. .tox/mojo/bin/activate
mojo --version
openstack --version

# OpenStack Mojo Spec Test Usage Example
mojo-project-new --series trusty --container containerless mojo-openstack-specs --mojo-root /tmp/mojo

mojo --mojo-root /tmp/mojo workspace-new --project mojo-openstack-specs --series trusty --stage specs/full_stack/stable_to_next/mitaka 'lp:~ost-maintainers/openstack-mojo-specs/mojo-openstack-specs' mojo-openstack-specs

mojo --mojo-root /tmp/mojo run --project mojo-openstack-specs --series trusty --stage specs/full_stack/stable_to_next/mitaka 'lp:~ost-maintainers/openstack-mojo-specs/mojo-openstack-specs
' osci-mojo
```

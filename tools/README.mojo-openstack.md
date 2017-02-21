# Install MOJO in a Python Virtual Env for OpenStack Specs

```
# Clone the tool to a tools directory
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
cd /home/ubuntu/tools/bot-control/tools

# Build venv
cd mojo-openstack
tox

# Source venv and use it
. .tox/mojo/bin/activate
mojo --version
openstack --version

mojo-project-new --series trusty --container containerless mojo-openstack-specs --mojo-root /tmp/mojo

mojo --mojo-root /tmp/mojo workspace-new --project mojo-openstack-specs --series trusty --stage specs/full_stack/stable_to_next/mitaka 'lp:~ost-maintainers/openstack-mojo-specs/mojo-openstack-specs' mojo-openstack-specs

mojo --mojo-root /tmp/mojo run --project mojo-openstack-specs --series trusty --stage specs/full_stack/stable_to_next/mitaka 'lp:~ost-maintainers/openstack-mojo-specs/mojo-openstack-specs
' osci-mojo
```

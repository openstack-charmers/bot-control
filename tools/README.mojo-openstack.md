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
```

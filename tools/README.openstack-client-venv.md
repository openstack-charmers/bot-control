# Install the latest OpenStack Client in a Python Virtual Env

```
# Clone the tool to a tools directory
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
cd /home/ubuntu/tools/bot-control/tools

# Build venv
cd openstack-client-venv
tox

# Source venv and use it
. .tox/openstack-client/bin/activate
openstack --version
```

Alternatively, or perhaps more traditionally, one can just:

```
cd /tmp
virtualenv openstack-client
. openstack-client/bin/activate
pip install python-openstackclient
openstack --version
```


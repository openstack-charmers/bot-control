juju-sym-switch
==============================================================================
```
usage: juju-sym-switch [-h] [--force] [-0] [-v] [-i] major_version

A system-wide sym link switching tool for Juju 1.x and 2.x binaries. Although
it is possible to set path values to control the Juju version, this is not
always effective with some tools which blindly call `juju` or `/usr/bin/juju`.
Targeted to Xenial (16.04) and later. Assumes that juju-1 and juju-2.0 are
installed. Does not attempt to install any packages.

positional arguments:
  major_version         Major version of Juju to switch to: 1 or 2

optional arguments:
  -h, --help            show this help message and exit

  --force               Force overwrite (destruction) of existing files or
                        symlinks. Understand that /usr/bin/juju needs to be
                        overwritten with a new symlink, and this is likely
                        required.

  -0, --dry             Dry run. Does not alter anything, even if --force is
                        specified.

  -v, --debug           Enable verbose debug output

  -i, --ignore-platform
                        Ignore platform operating system requirement
                        enforcement. Experimental, and will likely break
                        something!
```


#### Examples

This example presumes you are the ubuntu user, with sudo privileges, and that juju-1.25 and juju-2.0 are installed on your system (>= 16.04).  To confirm:
```
juju version
  2.0-beta15-xenial-amd64

juju-1.25 version
  1.25.5-xenial-amd64

juju-2.0 version
  2.0-beta15-xenial-amd64
```

```
# Clone the tool to a tools directory
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
cd /home/ubuntu/tools/bot-control/tools

# Dry run with debug output: set /usr/bin/juju as a symlink to /usr/bin/juju-2.0
./juju-sym-switch 2 -v0 --force
  
# Dry run with debug output: set /usr/bin/juju as a symlink to /usr/bin/juju-1.25
./juju-sym-switch 1 -v0 --force
```

```
# Doing it for real:

jenkins@juju-osci-machine-21:~/tools/bot-control/tools$ juju version
2.0-beta15-xenial-amd64

jenkins@juju-osci-machine-21:~/tools/bot-control/tools$ sudo ./juju-sym-switch 1 -v --force
DEBUG:root:Command arguments: {'dry': False, 'force': True, 'debug': True, 'major_version': '1'}
INFO:root:Dist OK: Ubuntu 16.04 xenial
DEBUG:root:R/W OK: /usr/bin/juju-1.25
DEBUG:root:R/W OK: /usr/bin/juju-2.0
DEBUG:root:R/W OK: /usr/bin/juju
DEBUG:root:R/W OK: /usr/bin/juju
DEBUG:root:Valid major version specified: 1
INFO:root:Juju version from /usr/bin/juju is 2.0-beta15-xenial-amd64, whereas you are seeking 1.2
WARNING:root:Removing existing file: /usr/bin/juju
INFO:root:Linking /usr/bin/juju-1.25 as /usr/bin/juju based on major version value 1
INFO:root:Juju version from /usr/bin/juju (1.25.6-xenial-amd64) validates with "1.2"

jenkins@juju-osci-machine-21:~/tools/bot-control/tools$ juju version
1.25.6-xenial-amd64

jenkins@juju-osci-machine-21:~/tools/bot-control/tools$ sudo ./juju-sym-switch 2 -v --force
DEBUG:root:Command arguments: {'debug': True, 'force': True, 'major_version': '2', 'dry': False}
INFO:root:Dist OK: Ubuntu 16.04 xenial
DEBUG:root:R/W OK: /usr/bin/juju-1.25
DEBUG:root:R/W OK: /usr/bin/juju-2.0
DEBUG:root:R/W OK: /usr/bin/juju
DEBUG:root:R/W OK: /usr/bin/juju
DEBUG:root:Valid major version specified: 2
INFO:root:Juju version from /usr/bin/juju is 1.25.6-xenial-amd64, whereas you are seeking 2.0
WARNING:root:Removing existing file: /usr/bin/juju
INFO:root:Linking /usr/bin/juju-2.0 as /usr/bin/juju based on major version value 2
INFO:root:Juju version from /usr/bin/juju (2.0-beta15-xenial-amd64) validates with "2.0"

jenkins@juju-osci-machine-21:~/tools/bot-control/tools$ juju version
2.0-beta15-xenial-amd64

```

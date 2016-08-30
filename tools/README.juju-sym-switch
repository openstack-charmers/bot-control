juju-sym-switch
==============================================================================
```
usage: juju-sym-switch [-h] [--force] [-0] [-v] major_version

A system-wide sym link switching tool for Juju 1.x and 2.x binaries. Although
it is possible to set path values to control the Juju version, this is not
always effective with some tools which blindly call `juju` or `/usr/bin/juju`.
Targeted to Xenial (16.04) and later. Assumes that juju-1 and juju-2.0 are
installed. Does not attempt to install any packages.

positional arguments:
  major_version  Major version of Juju to switch to: 1 or 2

optional arguments:
  -h, --help     show this help message and exit
  --force        Force overwrite (destruction) of existing files or symlinks.
                 Understand that /usr/bin/juju needs to be overwritten with a
                 new symlink, and this is likely required.
  -0, --dry      Dry run. Does not alter anything, even if --force is
                 specified.
  -v, --debug    Enable verbose debug output

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
To do it for real:

# Set /usr/bin/juju as a symlink to /usr/bin/juju-2.0
sudo ./juju-sym-switch 2 -v --force
juju version
  2.0-beta15-xenial-amd64
  
# Set /usr/bin/juju as a symlink to /usr/bin/juju-1.25
sudo ./juju-sym-switch 1 -v --force

juju version
  1.25.5-xenial-amd64

To do it for real, remove the 0.  May need to use sudo.
```

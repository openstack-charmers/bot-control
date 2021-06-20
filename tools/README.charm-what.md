#### Help
```
Usage: charm-what [options] DIRNAME

charm-what
==============================================================================
Attempt to identify whether a directory contains a Juju charm, charm layer,
charm interface, a built charm, or a source charm (top layer), based on
presence and/or absence of certain files.

Usage examples:
    ./charm-what DIRNAME


Options:
  -h, --help       show this help message and exit
  -d, -v, --debug  Enable debug logging.  (Default: False)
```

#### Examples

Presumes you are the ubuntu user.

##### Bash
```
mkdir -p /home/ubuntu/checkout
mkdir -p /home/ubuntu/tools
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
git clone https://github.com/openstack/charm-aodh /home/ubuntu/checkout/charm-aodh
cd /home/ubuntu/tools/bot-control/tools
./charm-what /home/ubuntu/checkout/charm-aodh
layer


./charm-what /home/ubuntu/checkout/charm-keystone
charm (classic)
```

##### Python
```
#!/usr/bin/python3     
import lib.charm_what.utils as cw_utils
print(cw_utils.whatis('/home/ubuntu/checkout/charm-aodh'))
```

##### Hacking / Testing

NOTE: this repo is really a collection of PoC and tactical scripts.  It is
intended to be refactored into a more proper python module at some point.

Some basic unit tests exist.  To check:

```
cd /home/ubuntu/tools/bot-control/tools
tox
```

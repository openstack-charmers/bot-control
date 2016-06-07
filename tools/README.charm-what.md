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

##### Bash
```
git clone https://github.com/openstack-charmers/bot-control /home/ubuntu/tools/bot-control
git clone https://github.com/openstack/charm-tempest /home/ubuntu/checkout/charm-tempest
cd /home/ubuntu/tools/bot-control/tools
./charm-what /home/ubuntu/checkout/charm-tempest
```

##### Python
```
#!/usr/bin/python3     
import lib.charm_what.utils as cw_utils
print(cw_utils.whatis('/home/ubuntu/checkout/charm-tempest'))
```

# bot-control: charm-single config file repository
## About
This repository tracks the non-default configuration requirements for each individual OpenStack charm, with regard to Ubuntu OpenStack Charm CI's "charm-single" functional smoke test.

The **charm-single** test exercises basic deployability of a single charm charm as an inexpensive and early sniff in the test pipeline.  It deploys a single charm in its default configuration, against multiple Ubuntu releases.

Some charms may require a bit of minimal non-default configuration values in order to succeed with this approach.  Or, it may be beneficial to set certain config options (such as installation resources) to a local resource to minimize WAN traffic in test automation.

Those configurations reside here, where deemed necessary on a per-charm basis.

**Beware:  Some config data may be specific to the UOSCI lab, but can be modified to fit other systems.**


## Structure
 - One file per charm name, such as:  ```charmname.yaml```
 - If no corresponding config file exists, the charm-single test deploys the charm purely with the default configuration values as defined in its config.yaml.

## Example 1
This is example illustrates a manual work flow.
#### Example ceph.yaml file:
```
ceph:
  options:
    fsid: "a1b2c3e4-a1b2-a1b2-a1b2-a1b2c3e4f500"
    monitor-secret: "ABCDefghIJKLMNopqrsTUVWxyz123456789000=="
```
#### Example usage:
```
  # Grab the configs
  mkdir -p $HOME/tools
  git clone https://github.com/openstack-charmers/bot-control.git $HOME/tools/bot-control --depth 1
  
  # Bootstrap, deploy, configure:
  juju bootstrap
  juju deploy ceph
  juju set ceph --config=$HOME/tools/bot-control/config/charm-single/ceph-stable.yaml
  
  # Inspect the service's resultant configuration data
  juju get ceph 
```

## Example 2:  Parse Environment Variables Into The Config

#### Example odl-controller.yaml file:
```
odl-controller:
  options:
    'install-url': ${AMULET_ODL_LOCATION},
    'http-proxy': ${AMULET_HTTP_PROXY},
    'https-proxy': ${AMULET_HTTP_PROXY},
```
#### Example usage:
```
  # Grab the configs
  mkdir -p $HOME/tools
  git clone https://github.com/openstack-charmers/bot-control.git $HOME/tools/bot-control --depth 1
  
  # Parse / expand environment variables (overwrites the YAML file!)
  # If an env var is not set, the value will be set empty.
  cd $HOME/tools/bot-control/tools
  ./expand_env_vars.py $HOME/tools/bot-control/config/charm-single/odl-controller.yaml
  
  # Bootstrap, deploy, configure:
  juju bootstrap
  juju deploy ceph
  juju set ceph --config=$HOME/tools/bot-control/config/charm-single/odl-controller.yaml
  
  # Inspect the service's resultant configuration data
  juju get ceph 
```

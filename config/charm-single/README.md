# bot-control: charm-single config file repository
## About
This repository tracks the non-default configuration requirements for each individual OpenStack charm, with regard to Ubuntu OpenStack Charm CI's "charm-single" functional smoke test.

The **charm-single** test exercises basic deployability of a single charm charm as an inexpensive and early sniff in the test pipeline.  It deploys a single charm in its default configuration, against multiple Ubuntu releases.

Some charms may require a bit of minimal non-default configuration values in order to succeed with this approach.  It may also be desired to set certain config options (such as installation resources) to a local resource to minimize WAN traffic in test automation.

Those configurations reside here, where deemed necessary on a per-charm basis.  Some config data may be specific to the UOSCI lab, but can be modified to fit other systems.

## Files
   - One yaml file per charm name in the `bot-control/config/charm-single/` directory.
   - Config files should only exist for charms where config overrides are necessary in the context of the charm-single test.
   - If no config file exists for a charm, the charm-single test deploys the charm purely with the default configuration values as defined in its config.yaml.

## Tools
The `bot-control/tools/env-render` tool is a generic utility which can be used to expand environment variables inside text files which contain jinja2 template directives, per the following examples.

---
## YAML File Example 1:  Fictional Foo Charm
* Statically define the charm configuration options to set.
* Ex. `bot-control/config/charm-single/foo.yaml`:
```
foo:
  knob: 4
  lever: False
  button: True
```

## YAML File Example 2:  ODL Charms
* By default, the ODL charms reach out to the internet to download a sizable installer tarball.  With iterative testing, that could cause undesirable WAN contention.  To minimize internet bandwidth consumption with repetitive testing, UOSCI houses the ODL installer tarball as a blob in a local object store bucket, but that could be any http server.

* The UOSCI test environment has restricted egress access, as do many enterprise networks.  An http proxy value is passed to the charm for use in retrieving any other required installation bits which are pre-authorized by that specific http proxy service.

* Any of the following bash environment variables that are set at test runtime will be rendered by the charm-single test runner using the jinja2 module before deploying and configuring the Juju service units.

* If one or more of the specified environment variables are not set, the template will render empty values for those configuration options.  To ensure successful configuration in cases where the required environment variables are not set, see the Ceph Charm Example.

* Ex. `bot-control/config/charm-single/odl-controller.yaml`:
```
odl-controller:
  install-url: {{ env['AMULET_ODL_LOCATION'] }}
  http-proxy: {{ env['AMULET_HTTP_PROXY'] }}
  https-proxy: {{ env['AMULET_HTTP_PROXY'] }}
```

## YAML File Example 3:  Ceph Charm
* The ceph charm will not successfully deploy without the *fsid* and *monitor-secret* charm configuration options.
* This provide those config option values.
* If the environment variables are set, those values are used.
* If the environment variables are not set, the specified static values are used.
* Ex. `bot-control/config/charm-single/ceph.yaml`:
```
ceph:
  fsid: {{ env['TEST_CEPH_FSID'] or '11111111-2222-3333-4444-555555555555' }}
  monitor-secret: {{ env['TEST_CEPH_MON_SEC'] or 'AQCXrnZQwI7KGBAAiPofmKEXKxu5bUzoYLVkbQ==' }}
```
---

## Usage Example:  Rendered Config from Environment Variables
Apply the environment variables to the charm config options as defined in `ceph.yaml`, then deploy and apply the resultant configuration from that rendered yaml file.
```
  # Grab the configs
  mkdir -p $HOME/tools
  git clone https://github.com/openstack-charmers/bot-control.git $HOME/tools/bot-control --depth 1

  # Set an environment variable
  export TEST_CEPH_FSID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

  # Render the config (inject env vars into template)
  $HOME/tools/bot-control/tools/env-render bot-control/config/charm-single/ceph.yaml
```

The `bot-control/config/charm-single/ceph.yaml` will look like this at this point, which is something we can feed to `juju set`:
```
ceph:
  fsid: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
  monitor-secret: AQCXrnZQwI7KGBAAiPofmKEXKxu5bUzoYLVkbQ==
```
Continue on...
```
  # Bootstrap, deploy, configure:
  juju bootstrap
  juju deploy ceph
  juju set ceph --config=$HOME/tools/bot-control/config/charm-single/ceph.yaml
  
  # Inspect the service's resultant configuration data
  juju get ceph
```

## Usage Example:  Static Config
Because the foo yaml file contains no variable templating, it is not necessary to use the `env-render` tool in this work flow.
```
  # Grab the configs
  mkdir -p $HOME/tools
  git clone https://github.com/openstack-charmers/bot-control.git $HOME/tools/bot-control --depth 1
  
  # Bootstrap, deploy, configure:
  juju bootstrap
  juju deploy foo
  juju set foo --config=$HOME/tools/bot-control/config/charm-single/foo.yaml
  
  # Inspect the service's resultant configuration data
  juju get foo
```

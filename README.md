# bot-control

## About
Ubuntu OpenStack Charm CI (UOSCI) control files, config files, and tool repositories for charm test automation.

## Structure
* bot-control/config/*:
    * Non-sensitive configuration data and templates related to test automation.
    * [README: charm-single](config/charm-single/README.md)
* bot-control/tools/*
    * Stand-alone tools related to test automation tasks.
    * [README: bundle-reducer](tools/README.bundle-reducer.md)
    * [README: charm-what](tools/README.charm-what.md)
    * [README: env-render](tools/README.env-render.md)
    * [README: ini-splice](tools/README.ini-splice.md)
    * [README: jenkins-job-builder - one way to use it](tools/README.jenkins-job-builder.md)
    * [README: juju-sym-switch](tools/README.juju-sym-switch.md)
    * [README: mojo-openstack](tools/README.mojo-openstack.md)
    * [README: openstack-client-venv](tools/README.openstack-client-venv.md)
    * [README: port-cleanup](tools/README.port-cleanup.md)

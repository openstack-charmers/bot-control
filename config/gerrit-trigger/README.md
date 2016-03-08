# bot-control: gerrit-trigger files
## About
These files are not actively consumed at this point.

## Control Files for OpenStack Charms Test Bots
- tools/ dir:  code, tools, scripts (no data / config)
- control/ dir:  config data
## UOSCI Jenkins test pipeline gerrit project trigger data files:
  - One line per project, one line per branch specification.  ex. ```p=openstack/charm-*```
  - ```test_charm_pipeline_trigger_projects.txt```
### Gerrit Trigger File Syntax:
 - From https://wiki.jenkins-ci.org/display/JENKINS/Gerrit+Trigger:
```
p=some/project
b^**/master/*
t~.*
f~\.txt$
p=some/other/project
b^**
Legend:
p for project
b for branch
t for topic
f for file
o for forbidden file
= for plain syntax
^ for ANT style syntax
~ for regexp syntax
Branch, topic, file and forbidden file lines are assumed to be part of the closest preceding project line.
```

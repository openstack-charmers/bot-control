#!/bin/bash -ex
# Build jenkins jobs from the definitions in this directory. The jjb-run.conf
# file must be created and populated with creds and url beforehand.

stat -t .tox/jjb/bin/activate ||\
    tox

. jjbrc

# Build everything
time jenkins-jobs --conf jjb-run.conf update --workers 16 .

# Other examples for limiting
# time jenkins-jobs --conf jjb-run.conf update --workers 16 . charm_push*
# time jenkins-jobs --conf jjb-run.conf update --workers 16 . bundle-*

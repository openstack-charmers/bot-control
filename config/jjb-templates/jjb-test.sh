#!/bin/bash -ex
# Perform a no-op test exercise of the jenkins job builder definitions

stat -t .tox/jjb/bin/activate ||\
    tox

. jjbrc

tempfile="$(mktemp)"

time jenkins-jobs --conf jjb-run.conf test . &> $tempfile
grep "jenkins_jobs" $tempfile

rm -fv $tempfile

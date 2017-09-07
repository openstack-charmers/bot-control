#!/bin/bash -ex
# Perform a no-op test exercise of the jenkins job builder definitions

stat -t .tox/jjb/bin/activate ||\
    tox

. jjbrc

tempfile="$(mktemp)"

time jenkins-jobs --conf jjb-run.conf test . &> $tempfile
jobs="$(awk '/jenkins_jobs.*bundle.*openstack/{ print $3 }' $tempfile)"

for j in $jobs; do
    time jenkins-jobs --conf jjb-run.conf delete -j $j
done

rm -fv $tempfile

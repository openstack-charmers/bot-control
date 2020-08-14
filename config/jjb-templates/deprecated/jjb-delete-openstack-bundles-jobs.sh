#!/bin/bash -ex
# Delete specifically-named jobs

stat -t .tox/jjb/bin/activate ||\
    tox

. jjbrc

tempfile="$(mktemp)"

time jenkins-jobs test . &> $tempfile
jobs="$(awk '/jenkins_jobs.*bundle.*openstack/{ print $3 }' $tempfile)"

for j in $jobs; do
    time jenkins-jobs delete -j $j
done

rm -fv $tempfile

#!/bin/bash -ex
# Perform a no-op test exercise of the jenkins job builder definitions

stat -t .tox/jjb/bin/activate ||\
    tox

. jjbrc

outfile="./output.txt"

time jenkins-jobs --conf jjb-run.conf test . &> $outfile
grep "jenkins_jobs" $outfile

echo "Examine $outfile for details"

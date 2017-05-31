#!/bin/bash -ex

# Basic yaml syntax check
yaml_files=$(find -iregex '.*\.\(yaml\|yml\)$')
for yaml_file in $yaml_files; do
    echo "$yaml_file"
    /usr/bin/env python3 -c 'import yaml,sys;yaml.safe_load(sys.stdin)' < $yaml_file
done

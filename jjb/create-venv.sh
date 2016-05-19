#!/bin/bash -e
# Set up a python virtual env and install jenkins job builder to it.
#
# Usage example:
#   ./create-venv.sh
#   source .venv/bin/activate
#   jenkins-jobs --help
#
# To exit the virtual env:
#   deactivate
#
virtualenv .venv
source .venv/bin/activate
pip install -r requirements.txt

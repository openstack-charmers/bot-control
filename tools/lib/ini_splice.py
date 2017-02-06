#!/usr/bin/env python3
#
# Copyright 2016 Canonical Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""
An INI configuration file parser. Currently only supports replacing
entire sections.  Usage example:
  `ini-splice tox.ini replace-section testenv from gold.ini`
"""

import argparse
import configparser
import logging
import os
import sys

from collections import OrderedDict
from copy import deepcopy

VALID_ACTIONS = [
    'replace-section'
]

# TODO: make this more useful with additional actions, such as:
#    - replace specific key values within particular sections
#    - remove a section
#    - remove specific keys from particular sections
#    And so on.


def validate_config(cli_conf):
    """Check cli config and system."""

    assert os.path.isfile(cli_conf['ini_file']) is True,\
        'File not found: {}'.format(cli_conf['ini_file'])

    assert os.path.isfile(cli_conf['other_file']) is True,\
        'File not found: {}'.format(cli_conf['other_file'])

    assert os.access(cli_conf['ini_file'], os.R_OK and os.W_OK) is True,\
        'File not read/write: {}'.format(cli_conf['ini_file'])

    assert cli_conf['action'] in VALID_ACTIONS,\
        'Invalid action: {}.  Expecting one of: {}'.format(cli_conf['action'],
                                                           VALID_ACTIONS)


def cli_args():
    """Command line arguments"""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('ini_file',
                        help='INI file to edit.')
    parser.add_argument('action',
                        help='Action to take. Currently supported '
                             'actions: replace-section.')
    parser.add_argument('target',
                        help='Target on which to take action. Currently '
                             'supported target types:  section name.')
    parser.add_argument('from',
                        help='A required positional.')
    parser.add_argument('other_file',
                        help='INI file to read and use in the context of '
                             'the action.')
    parser.add_argument('-0', '--dry',
                        help='Dry run.  Does not alter anything.',
                        action='store_true', required=False)
    parser.add_argument('-v', '--debug',
                        help='Enable verbose debug output.',
                        action='store_true', required=False)
    try:
        options = parser.parse_args()
    except:
        parser.print_help()
        sys.exit(0)

    return options


def set_logging(cli_conf):
    """Set logging."""
    if cli_conf['debug']:
        log_level = logging.DEBUG
    else:
        log_level = logging.INFO
    logging.basicConfig(level=log_level)


def read_ini(file_name):
    """Read ini file, return configparser instance."""
    config = configparser.ConfigParser()
    config.read(file_name)
    return config


def write_ini(file_name, config):
    """Write ini file with configparser instance data."""
    with open(file_name, 'w') as _file:
        config.write(_file)
    return config


def ini_to_dict(config):
    """Read a configparser instance, return dict of
       sections and key value pairs.
    """
    sections = config.sections()
    dict_data = OrderedDict()

    for section in sections:
        dict_data[section] = {}
        for key, val in config.items(section):
            dict_data[section][key] = val
    return dict_data


def dict_to_ini(dict_data):
    """Read dictionary data, return a configparser instance,
       populated with sections and key value pairs.
    """
    config = configparser.ConfigParser()
    config.read_dict(OrderedDict(dict_data))
    return config


def replace_section(ini_file, target, other_file, dry, **kwargs):
    """Replace an entire section of an ini file with the contents
       of the same-named section from another ini file.
    """
    dict_data = ini_to_dict(read_ini(ini_file))
    other_dict = ini_to_dict(read_ini(other_file))
    dict_data[target] = deepcopy(other_dict[target])

    action_summary = ('Replace the {} section in {} with the {} section from '
                      '{}.'.format(target, ini_file, target, other_file))
    if not dry:
        logging.info(action_summary)
        write_ini(ini_file, dict_to_ini(dict_data))
    else:
        logging.info('Dry run for: {}'.format(action_summary))


if __name__ == '__main__':
    cli_conf = vars(cli_args())
    set_logging(cli_conf)
    logging.debug('Command arguments: {}'.format(cli_conf))
    validate_config(cli_conf)

    if cli_conf['action'] == 'replace-section':
        replace_section(**cli_conf)

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
A sym link switching tool for Juju 1.x and 2.x binaries.  Although it is
possible to set path values to control the Juju version, this is not
effective with some tools which blindly call `juju` or `/usr/bin/juju`.

Targeted to Xenial (16.04) and later.
"""

import argparse
import logging
import os
import platform
import subprocess
import sys

DESCRIPTION = ("A system-wide sym link switching tool for Juju 1.x and 2.x "
               "binaries. Although it is possible to set path values to "
               "control the Juju version, this is not always effective with "
               "some tools which blindly call `juju` or `/usr/bin/juju`. "
               "Targeted to Xenial (16.04) and later.  Assumes that juju-1 "
               "and juju-2.0 are installed. Does not attempt to install any "
               "packages.")

# Xenial binaries and symlinks with both Juju 1 and 2 installed:
#   lrwxrwxrwx 1 root root   8 Aug 23 13:12 /usr/bin/juju -> juju-2.0
#   lrwxrwxrwx 1 root root   9 Aug 16 20:04 /usr/bin/juju-1 -> juju-1.25
#   -rwxr-xr-x 1 root root  68 Aug 16 20:04 /usr/bin/juju-1.25
#   -rwxr-xr-x 1 root root  67 Aug 23 13:12 /usr/bin/juju-2.0


MAP = {
    '1': {
        'src': '/usr/bin/juju-1.25',
        'dst': '/usr/bin/juju',
        'check': '1.2',
    },
    '2': {
        'src': '/usr/bin/juju-2.0',
        'dst': '/usr/bin/juju',
        'check': '2.0',
    },
    'default': '1'
}


def validate_config(conf):
    """Check config and system."""
    assert check_platform_dist_supported(conf) is True
    assert check_files() is True
    assert check_major_version_value(conf) is True


def check_files():
    """Ensure the required binaries exist and that they can be
       read and written.
    """
    expect_files = [
        MAP['1']['src'],
        MAP['2']['src'],
        MAP['1']['dst'],
        MAP['2']['dst'],
    ]

    for _file in expect_files:
        if not os.path.isfile(_file):
            logging.error('File {} not found'.format(_file))
            return False
        if not os.access(_file, os.R_OK and os.W_OK):
            logging.error('File {} is not read/write as user'
                          ': {}'.format(_file, os.getlogin()))
            return False
        logging.debug('R/W OK: {}'.format(_file))
    return True


def check_major_version_value(conf):
    """Check major_version argument against MAP keys."""
    _check = conf['major_version']
    if _check not in MAP or _check in MAP and 'src' not in MAP[_check]:
        logging.error('Invalid major_version value: {}'.format(_check))
        return False
    logging.debug('Valid major version specified: {}'.format(_check))
    return True


def check_executable_version(major_ver):
    """Validate that the `juju version` output matches the check."""
    dst = MAP[major_ver]['dst']
    check = MAP[major_ver]['check']
    juju_version = get_juju_version(bin=dst)
    if juju_version[:len(check)] != check:
        logging.info('Juju version from {} is {}, whereas you are seeking '
                     '{}'.format(dst, juju_version.strip(), check))
        return False

    logging.info('Juju version from {} ({}) validates with '
                 '"{}"'.format(dst, juju_version.strip(), check))
    return True


def check_platform_dist_supported(conf):
    """Ensure minimum operating system requirement is met."""
    distro, version, release = platform.dist()

    if (distro != 'Ubuntu' or float(version) < 16.04 and not
            conf['ignore_platform']):
        logging.error('This tool supports Ubuntu 16.04 and later.  Detected: '
                      '{} {} {}'.format(distro, version, release))
        return False
    elif (distro != 'Ubuntu' or float(version) < 16.04 and
            conf['ignore_platform']):
        logging.warning('Ignoring unsupported OS version!  Use at your own '
                        'risk.  This tool supports Ubuntu 16.04 and later. '
                        'Detected: {} {} {}'.format(distro, version, release))
        return True

    logging.info('Dist OK: {} {} {}'.format(distro, version, release))
    return True


def get_juju_version(bin=None):
    """ Get juju version.  Default to the juju command in the user path.
        Optionally specify an alternate bin.
    """
    if not bin:
        bin = 'juju'
    return subprocess.check_output([bin, 'version']).decode('UTF-8')


def update_juju_symlinks(conf={}):
    """ Update system symlinks based on major version value.
    """
    if not conf:
        raise

    major_ver = conf['major_version']
    src = MAP[major_ver]['src']
    dst = MAP[major_ver]['dst']

    if check_executable_version(major_ver) is True:
        logging.info('No action taken - symlinks are already in order.')
        sys.exit(0)

    if os.path.lexists(dst) and conf['force'] and not conf['dry']:
        logging.warn('Removing existing file: {}'.format(dst))
        os.remove(dst)

    elif os.path.lexists(dst) and conf['force'] and conf['dry']:
        logging.warn('Dry run - would have removed: {}'.format(dst))

    elif os.path.lexists(dst) and not conf['force']:
        logging.error('File exists. Use force to overwrite {}.  '
                      'See --help.'.format(dst))
        sys.exit(1)

    if not conf['dry']:
        logging.info('Linking {} as {} based on major version '
                     'value {}'.format(src, dst, major_ver))
        os.symlink(src, dst)
    else:
        logging.info('Dry run - would have linked {} as {} based on major '
                     'version value {}'.format(src, dst, major_ver))

    # This should not happen.  But catch it if it does.
    if check_executable_version(major_ver) is not True and not conf['dry']:
        raise ValueError('The version reported by Juju mismatches the '
                         'expectation, even after symlinking it.')


def cli_args():
    """Command line arguments"""
    parser = argparse.ArgumentParser(description=DESCRIPTION)
    parser.add_argument('major_version',
                        help='Major version of Juju to switch to: 1 or 2')
    parser.add_argument('--force',
                        help='Force overwrite (destruction) of existing '
                             'files or symlinks. Understand that '
                             '/usr/bin/juju needs to be overwritten with '
                             'a new symlink, and this is likely required.',
                        action='store_true', required=False)
    parser.add_argument('-0', '--dry',
                        help='Dry run.  Does not alter anything, even if '
                             '--force is specified.',
                        action='store_true', required=False)
    parser.add_argument('-v', '--debug',
                        help='Enable verbose debug output',
                        action='store_true', required=False)
    parser.add_argument('-i', '--ignore-platform',
                        help='Ignore platform operating system requirement '
                             'enforcement.  Experimental, and will likely '
                             'break something!',
                        action='store_true', required=False)
    try:
        options = parser.parse_args()
    except:
        parser.print_help()
        sys.exit(0)

    return options


def set_debug(conf):
    """Set debug."""
    if conf['debug']:
        log_level = logging.DEBUG
    else:
        log_level = logging.INFO
    logging.basicConfig(level=log_level)


if __name__ == '__main__':
    conf = vars(cli_args())
    set_debug(conf)
    logging.debug('Command arguments: {}'.format(conf))
    validate_config(conf)
    sys.exit(update_juju_symlinks(conf))

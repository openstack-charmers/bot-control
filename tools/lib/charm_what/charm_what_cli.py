#!/usr/bin/python3

# This file is part of UOSCI bot-control.
#
# Author, maintainer:  Ryan Beisner <ryan.beisner@canonical.com>
#
# Copyright 2016 Canonical Ltd.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3, as
# published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranties of
# MERCHANTABILITY, SATISFACTORY QUALITY, or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import logging
import optparse
import os
import sys
import yaml
import lib.charm_what.utils as cw_utils

USAGE = '''Usage: %prog [options] DIRNAME

%prog
==============================================================================
Attempt to identify whether a directory contains a Juju charm, charm layer,
charm interface, or a built charm, based on presence and/or absence of certain
files.

Usage examples:
    ./%prog DIRNAME
'''


def option_handler():
    '''Define and handle command line parameters
    '''
    # Define command line options
    parser = optparse.OptionParser(USAGE)
    parser.add_option('-d', '-v', '--debug',
                      help='Enable debug logging.  (Default: False)',
                      dest='debug', action='store_true',
                      default=False)

    params = parser.parse_args()
    (opts, args) = params

    # Handle parameters, inform user
    if opts.debug:
        logging.basicConfig(level=logging.DEBUG)
        logging.info('Logging level set to DEBUG!')
        logging.debug('opts: \n{}'.format(
            yaml.dump(vars(opts), default_flow_style=False)))
        logging.debug('args: \n{}'.format(
            yaml.dump(args, default_flow_style=False)))
    else:
        logging.basicConfig(level=logging.INFO)

    # Validate parameters
    if len(args) > 2:
        parser.print_help()
        logging.error('Too many arguments. Expecting at least 1, '
                      'no more than 2.')
        sys.exit(1)

    if not args:
        parser.print_help()
        logging.error('Missing a required argument: input file')
        sys.exit(1)

    if not os.path.isdir(args[0]):
        logging.error('Unable to open directory: {}'.format(args[0]))
        sys.exit(1)

    return (opts, args)


# And, go.
def main():
    opts, args = option_handler()
    asset_type = cw_utils.whatis(args[0])
    print('{}'.format(asset_type))

if __name__ == '__main__':
    main()

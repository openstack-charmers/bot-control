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


import logging
import optparse
import os
import sys
import yaml
import charm_what.utils as cw_utils

USAGE = '''Usage: %prog [options] DIRNAME

%prog
==============================================================================
Attempt to identify whether a directory contains a Juju charm, charm layer,
charm interface, a built charm, or a source charm (top layer), based on
presence and/or absence of certain files.

Usage examples:
    ./%prog DIRNAME
'''


def option_handler():
    '''Define and handle command line parameters.
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
    if asset_type:
        print(asset_type)
    else:
        print('{} is not a charm, interface or layer'.format(args[0]))
        sys.exit(1)


if __name__ == '__main__':
    main()

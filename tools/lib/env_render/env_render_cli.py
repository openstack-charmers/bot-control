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
import common.tools_common as u

USAGE = '''Usage: %prog [options] FILENAME [OPTIONAL ENV VAR LIST]

%prog
==============================================================================
Inject environment variables into a file using jinja2-style template syntax.

Usage examples:

  Given the following FILENAME contents (ie. template):
    Dear {{ env['USER'] or "Ubuntu user" }},
    My favorite type of bacon is {{ env['FAV_BACON'] or "any bacon" }}.
    See you at {{ env['HOME'] or "home" }}.
    <eof>

  Expand all matches using all available environment variables:
    ./%prog FILENAME

  Expand all matches using only the listed environment variables:
    ./%prog FILENAME "HOME,USER"
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
    parser.add_option('-y', '--yes',
                      help='Do not ask for confirmation to overwrite.'
                      '  (Default: False)',
                      dest='confirm_overwrite', action='store_true',
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

    if len(args) == 2 and ' ' in args[1]:
        parser.print_help()
        logging.error('Spaces are not allowed in env var list.')
        sys.exit(1)

    if not args:
        parser.print_help()
        logging.error('Missing a required argument: input file')
        sys.exit(1)

    if not os.path.isfile(args[0]):
        parser.print_help()
        logging.error('Unable to open file: {}'.format(args[0]))
        sys.exit(1)

    return (opts, args)


# And, go.
def main():
    opts, args = option_handler()

    target = args[0]
    source = os.path.basename(args[0])
    templates_dir = os.path.dirname(args[0])
    env_vars = os.environ

    if len(args) == 2:
        # Render only the specified env vars
        env_var_limit_list = args[1].split(',')
        logging.debug('Limiting to environment variable, '
                      'if they are set: {}'.format(env_var_limit_list))
        context = {
            'env': {
                k: v for k, v in env_vars.items() if k in env_var_limit_list
            }
        }
        logging.debug('Limited context values: {}'.format(context))
    else:
        # Render with all available env vars
        logging.info('Using all available environment variables.')
        context = {
            'env': env_vars
        }

    # Require confirmation or the yes flag to overwrite
    question = 'File ({}) will be overwritten.  Proceed?'.format(target)
    if not opts.confirm_overwrite and not u.prompt_yes_no(question):
        logging.error('Overwrite not confirmed.  Not rendering file.')
        sys.exit(1)

    # Render it
    try:
        logging.info('Rendering {}'.format(target))
        u.render(source, target, context, templates_dir)
        f = open(target, 'a')
        f.write('\n')
        f.close()
    except:
        logging.error('Failed to render file: {}'.format(target))
        raise


if __name__ == '__main__':
    main()

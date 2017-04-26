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
A simple tool to collect log files from Juju all application units in the
current Juju model.

Usage examples:
    ./%prog /tmp/log_dir

    ./%prog $WORKSPACE/logs

    ./%prog $(mktemp -d)

    ./%prog -v0
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

    parser.add_option('-0', '--dry-run',
                      help='Dry run, do not actually do anything. '
                            '(Default: False)',
                      dest='dry_run', action='store_true',
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
    if len(args) != 1 and not opts.dry_run:
        parser.print_help()
        logging.error('Expected one command argument: <log_dir>')
        sys.exit(1)

    return (opts, args)


# And, go.
def main():
    opts, args = option_handler()

    if opts.dry_run:
        log_dir = None
    else:
        log_dir = args[0]
        if not os.path.exists(log_dir):
            u.safe_mkdir(log_dir)

    app_units = u.get_juju_application_units()
    logging.info('Application units: {}'.format(app_units))

    for app_unit in app_units:
        cmds = []
        app_unit_clean_name = app_unit.replace('/', '-')
        cmds.append('juju ssh {} sudo tar -cjf '
                    '/home/ubuntu/{}-var-log.tar.bz2 /var/log '
                    '--warning=no-file-changed'.format(
                        app_unit,
                        app_unit_clean_name))

        cmds.append('juju ssh {} sudo tar -cjf '
                    '/home/ubuntu/{}-etc.tar.bz2 /etc '
                    '--warning=no-file-changed '
                    '--exclude="/etc/X11" --exclude="/etc/ssl" '
                    '--exclude="/etc/ssh" --exclude="shadow"'.format(
                        app_unit,
                        app_unit_clean_name))

        # May not always exist (ceph stores some confs here)
        cmds.append('juju ssh {} "[[ -d /var/lib/charm ]] && '
                    'sudo tar -cjf /home/ubuntu/{}-var-lib-charm.tar.bz2 '
                    '/var/lib/charm ||:"'.format(app_unit,
                                                 app_unit_clean_name))

        cmds.append('juju ssh {} "dpkg -l | bzip2 -9z > '
                    '/home/ubuntu/{}-dpkg-list.bz2"'.format(
                        app_unit, app_unit_clean_name))

        cmds.append('juju ssh {} "df -h | bzip2 -9z > '
                    '/home/ubuntu/{}-df.bz2"'.format(
                        app_unit, app_unit_clean_name))

        cmds.append('juju ssh {} "ps aux | bzip2 -9z > '
                    '/home/ubuntu/{}-processes.bz2"'.format(
                        app_unit, app_unit_clean_name))

        cmds.append('juju ssh {} "sudo netstat -taupn | '
                    'grep LISTEN | bzip2 -9z > '
                    '/home/ubuntu/{}-listening.bz2"'.format(
                        app_unit, app_unit_clean_name))

        cmds.append('juju scp {}:/home/ubuntu/*.bz2'
                    ' {}'.format(app_unit, log_dir))

        if not u.run_cmds(cmds, dry_run=opts.dry_run):
            logging.error('Failed to get logs from application unit: '
                          '{}'.format(app_unit))

    return 0


if __name__ == '__main__':
    main()

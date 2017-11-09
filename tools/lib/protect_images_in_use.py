#!/usr/bin/env python3
#
# Copyright 2017 Canonical Ltd
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


"""A tool to protect images in use"""

import argparse
import logging
import sys

from common.classes import HashableImage
from common.tools_common import (
    get_image_ids_in_use,
    set_debug,
    validate_config,
)

DESCRIPTION = sys.modules[__name__].__doc__

# Usage examples:

# Test as non-admin (no intervention)
# ./protect-images

# No-intervention query only as admin
# ./protect-images --admin

# Set protected/unprotected
# ./protect-images --admin --protect


def set_image_properties(conf, clients):
    """Set image protection properties

    This function will query all images, check for images in use and set images
    in use to protected and those not in use to unprotected.

    :param conf: Argparse configuration dictionary
    :param clients: Clients dictionary from get_openstack_clients
    :return: None
    """
    glance_images = clients['gl'].images.list()
    # Use hashable images for set math
    images = []
    for image in glance_images:
        images.append(HashableImage(image))
    logging.info("Total number of images: {}".format(len(images)))
    image_ids_in_use = get_image_ids_in_use(conf, clients)
    logging.info("Number of images in use: {}".format(len(image_ids_in_use)))

    protected = []
    unprotected = []
    for image in images:
        if conf['admin'] is True:
            if image.id in image_ids_in_use:
                if not conf['quiet']:
                    logging.info("Protecting image {} {}"
                                 "".format(image.name, image.id))
                if conf['images_protect'] is True:
                    # Use the glance Image class for the update
                    clients['gl'].images.update(image.image, protected=True)
                protected.append(image.id)
            else:
                if not conf['quiet']:
                    logging.info("Unprotected image {} {}".format(image.name,
                                                                  image.id))
                if conf['images_protect'] is True:
                    # Use the glance Image class for the update
                    clients['gl'].images.update(image.image, protected=False)
                unprotected.append(image.id)
    logging.info("Number of images protected: {}".format(len(protected)))
    logging.info("Number of images unprotected: {}".format(len(unprotected)))
    return 0


def cli_args():
    """Command line arguments"""
    parser = argparse.ArgumentParser(description=DESCRIPTION)
    parser.add_argument('-v', '--debug',
                        help='Enable verbose debug output',
                        action='store_true', required=False)

    parser.add_argument('-q', '--quiet',
                        help='Slightly less verbose, but not silent',
                        action='store_true', required=False)

    parser.add_argument('--protect',
                        dest='images_protect',
                        help=('Protect images in use. Note: '
                              '--admin switch is also required to execute '
                              'protect updates. (USE WITH CAUTION)'),
                        action='store_true', required=False, default=False)

    parser.add_argument('--admin',
                        dest='admin',
                        help=('Set administrator. Required to execute updates '
                              'with the --protect switch (USE WITH CAUTION). '
                              'Note: non-admin queries will underestimate the '
                              'number of images to be protected due to '
                              'limited visibility into images in use.'),
                        action='store_true', required=False, default=False)

    options = parser.parse_args()
    return options


if __name__ == '__main__':
    conf = vars(cli_args())
    set_debug(conf)
    logging.info('Command arguments: {}'.format(conf))
    clients = validate_config()
    sys.exit(set_image_properties(conf, clients))

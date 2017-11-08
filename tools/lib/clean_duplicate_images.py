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


"""A tool to delete duplicate glance images not in use."""

import argparse
import copy
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

# Test as non-admin (non-destructive)
# ./image-duplicate-cleanup

# Non-destructive query only as admin
# ./image-duplicate-cleanup --admin

# Destructive deletes
# ./image-duplicate-cleanup --admin --delete


def get_duplicate_images(images):
    """Find duplicate images

    :param images: List of HashableImage class instances
    :return: Dictionary of duplicate images of the form:
        {string_image_name: [
                         dup_hashable_image_instance_1,
                         dup_hashable_image_instance_2,
                         dup_hashable_image_instance_3]}
    """
    images_dict = {}
    for image in images:
        try:
            images_dict[image.name].append(image)
        except KeyError:
            images_dict[image.name] = [image]

    all_images = copy.copy(images_dict)
    for key, value in all_images.items():
        if len(value) == 1:
            images_dict.pop(key)

    return images_dict


def do_cleanup(conf, clients):
    """Delete (or query) duplicate images not in use

    This function will query all images, check for duplicates, check for images
    in use and finally, delete those duplicate images that are not in use.

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
    duplicate_images_dict = get_duplicate_images(images)

    for_deletion = []
    num_of_dups = 0
    for dup_images in duplicate_images_dict.values():
        _in_use = set()
        num_of_dups += len(dup_images)
        for image in dup_images:
            if image.id in image_ids_in_use:
                _in_use.add(image)
        if _in_use:
            # Delete all but those in use
            dup_images = set(dup_images) - _in_use
        else:
            # Delete all but one
            dup_images = dup_images[1:]
        for_deletion.extend(dup_images)

    logging.info("Number of duplicates: {}".format(num_of_dups))
    logging.info("Number of deletes queued: {}".format(len(for_deletion)))
    for image in for_deletion:
        if conf['image_delete'] is True and conf['admin'] is True:
            if not conf['quiet']:
                logging.info("Deleting image {} {}"
                             "".format(image.name, image.id))
            # Use the glance Image class for the deletion
            clients['gl'].images.delete(image.image)
        elif not conf['quiet']:
            logging.info("Would have deleted image {} {}".format(image.name,
                                                                 image.id))
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

    parser.add_argument('--delete',
                        dest='image_delete',
                        help=('Delete duplicate images not in use. Note: '
                              '--admin switch is also required to execute '
                              'deletes. (USE WITH CAUTION)'),
                        action='store_true', required=False, default=False)

    parser.add_argument('--admin',
                        dest='admin',
                        help=('Set administrator. Required to execute deletes '
                              'with the --delete switch (USE WITH CAUTION). '
                              'Note: non-admin queries will overestimate the '
                              'number of deletes queued due to limited '
                              'visibility into images in use.'),
                        action='store_true', required=False, default=False)

    options = parser.parse_args()
    return options


if __name__ == '__main__':
    conf = vars(cli_args())
    set_debug(conf)
    logging.info('Command arguments: {}'.format(conf))
    clients = validate_config()
    sys.exit(do_cleanup(conf, clients))

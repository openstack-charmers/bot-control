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


class HashableImage(object):
    """The HashableImage class allows us to do set math on the images. The
    glance Image class is missing this simple feature. This greatly simplifies
    the code required.
    """

    def __init__(self, image):
        """Initialize HashableImage

        :param image: glanceclient Image class instance
        """
        self.image = image
        self.id = image.id
        self.name = image.name

    def __repr__(self):
        """Return string image.id
        """
        return self.id

    def __hash__(self):
        """Hash based on image.id
        """
        return hash(self.id)

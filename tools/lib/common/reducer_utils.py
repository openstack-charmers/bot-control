#!/usr/bin/python3
'''
bundle-reducer helpers
'''

import logging
import os
import random
import string
import yaml

from copy import deepcopy


def read_yaml(the_file):
    '''Returns yaml data from provided file name

    :param the_file: yaml file name to read
    :returns: dictionary of yaml data from file
    '''
    if not os.path.exists(the_file):
        raise ValueError('File not found: {}'.format(the_file))
    with open(the_file) as yaml_file:
        data = yaml.safe_load(yaml_file.read())
    return data


def write_yaml(data, the_file):
    '''Save yaml data dictionary to a yaml file

    :param the_file: yaml file name to write
    :returns: dictionary of yaml data to write to file
    '''
    with open(the_file, 'w') as yaml_file:
        yaml_file.write(yaml.dump(data, default_flow_style=False))


def recursive_dict_key_search(data, key):
    '''Recursively look for key match in dictionaries, return value.

    :param data: dictionary data
    :param key: key to find
    :returns: value of found key
    '''
    if key in data:
        return data[key]
    for _, val in data.items():
        if isinstance(val, dict):
            ret = recursive_dict_key_search(val, key)
            if ret is not None:
                return ret


# above, ripped from uosci common
# below, unique here, or improved from uosci common


def yaml_dump(data):
    '''Return dict data as pretty-ish yaml output.'''
    return yaml.dump(data, default_flow_style=False)


def rnd_str(length=32):
    '''Return lower-case alpha-numeric random string, a la pwgen.

    :param length: number of characters to generate
    :returns: lower-case alpha-numeric random string
    '''
    _str = ''
    for _ in range(length):
        _str += random.choice(string.ascii_lowercase + string.digits)
    return _str


def get_all_services(org_bundle_dict):
    '''Return a frozen set of all services values from dict.'''
    svcs_all = set()

    for (k, v) in org_bundle_dict.items():
        _svcs = recursive_dict_key_search(v, 'services')
        if _svcs:
            svcs_all |= set(list(_svcs.keys()))

    return frozenset(svcs_all)


def rm_attr_from_services(org_bundle_dict, attr):
    """Remove specified key from all services, where attr is
    commonly one of: to, constraints, or options."""

    new_bundle_dict = deepcopy(org_bundle_dict)

    for (k, v) in new_bundle_dict.items():
        if 'services' in v.keys():
            _svcs = v['services']
            for (_svc, _svc_attrs) in _svcs.items():
                if attr in _svc_attrs.keys():
                    logging.debug('Removing {} from '
                                  '{}'.format(attr, _svc))
                    del new_bundle_dict[k]['services'][_svc][attr]

    return new_bundle_dict


def extract_services(org_bundle_dict, svcs_include="ALL", svcs_exclude=None,
                     exclude_related=False, rm_constraints=False,
                     rm_placements=False):

    new_bundle_dict = deepcopy(org_bundle_dict)
    svcs_whitelist = set()

    # Multiple top-level targets can exist, each with potentially
    # having services and/or relations defined.

    # Make list of service names from all targets
    svcs_all = get_all_services(org_bundle_dict)
    logging.debug(yaml_dump({'svcs_all': sorted(list(svcs_all))}))

    # Determine other services
    svcs_other = svcs_exclude | svcs_all - svcs_include
    logging.debug(yaml_dump({'svcs_other': sorted(list(svcs_other))}))

    # Remove relations, optionally determine svcs whitelist
    for (k, v) in org_bundle_dict.items():
        if 'relations' in v.keys():
            _rels = v['relations']
            rels_whitelist = []
            for _rel_pair in _rels:
                _a = _rel_pair[0].split(':')[0]
                _b = _rel_pair[1].split(':')[0]
                if exclude_related:
                    if _a not in svcs_other and _b not in svcs_other and \
                            _a not in svcs_exclude and _b not in svcs_exclude:
                        rels_whitelist.append(_rel_pair)
                else:
                    if (_a not in svcs_other or _b not in svcs_other) and \
                            _a not in svcs_exclude and _b not in svcs_exclude:
                        svcs_whitelist |= {_a}
                        svcs_whitelist |= {_b}
                        rels_whitelist.append(_rel_pair)

            svcs_whitelist -= svcs_exclude

            if not rels_whitelist:
                logging.debug('Removing empty relations key from '
                              '{}.'.format(k))
                del new_bundle_dict[k]['relations']
            else:
                new_bundle_dict[k]['relations'] = rels_whitelist
                logging.debug('rels_whitelist for {}:'
                              '\n{}'.format(k, rels_whitelist))
    logging.debug('svcs_whitelist:\n{}'.format(svcs_whitelist))

    # Remove services
    for (k, v) in org_bundle_dict.items():
        if 'services' in v.keys():
            _svcs = set(v['services'])
            if _svcs & svcs_other - svcs_whitelist:
                for _s in _svcs & svcs_other - svcs_whitelist:
                    del new_bundle_dict[k]['services'][_s]
                    if not len(new_bundle_dict[k]['services']):
                        logging.debug('Removing empty services key from '
                                      '{}.'.format(k))
                        del new_bundle_dict[k]['services']

    # Remove placements
    if rm_placements:
        new_bundle_dict = rm_attr_from_services(new_bundle_dict,
                                                'to')

    # Remove constraints
    if rm_constraints:
        new_bundle_dict = rm_attr_from_services(new_bundle_dict,
                                                'constraints')

    return new_bundle_dict

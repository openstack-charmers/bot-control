#!/usr/bin/python3
'''
charm-what utils

Attempt to identify whether a directory contains a Juju charm, charm layer,
charm interface, a built charm, or a source charm (top layer), based on
presence and/or absence of certain files.
'''

import os


def f_exists(*args):
    return os.path.isfile(os.path.join(*args))


def is_built_charm(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm built from layers and/or interfaces.'''
    return (
        f_exists(asset_path, 'config.yaml') and
        f_exists(asset_path, 'metadata.yaml') and
        f_exists(asset_path, 'layer.yaml') and
        f_exists(asset_path, '.build.manifest') and not
        f_exists(asset_path, 'interface.yaml')
    )


def is_source_charm(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm source (containing a top layer).'''
    return (
        f_exists(asset_path, 'src', 'layer.yaml') and
        f_exists(asset_path, 'src', 'metadata.yaml') and
        f_exists(asset_path, 'src', 'config.yaml') and not
        f_exists(asset_path, 'src', '.build.manifest') and not
        f_exists(asset_path, 'src', 'interface.yaml') and not
        f_exists(asset_path, 'layer.yaml') and not
        f_exists(asset_path, '.build.manifest') and not
        f_exists(asset_path, 'interface.yaml')
    )


def is_classic_charm(asset_path):
    '''Return True if the contents of asset_path appear to
    be a classic (non-layered) Juju charm.'''
    return (
        f_exists(asset_path, 'metadata.yaml') and not
        f_exists(asset_path, 'layer.yaml') and not
        f_exists(asset_path, '.build.manifest') and not
        f_exists(asset_path, 'interface.yaml')
    )


def is_charm_interface(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm interface.'''
    return (
        f_exists(asset_path, 'interface.yaml') and not
        f_exists(asset_path, '.build.manifest') and not
        f_exists(asset_path, 'layer.yaml')
    )


def is_charm_layer(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm layer.'''
    return (
        f_exists(asset_path, 'layer.yaml') and not
        f_exists(asset_path, '.build.manifest') and not
        f_exists(asset_path, 'interface.yaml')
    )


def whatis(asset_path):
    '''Attempt to determine if the contents of asset_path appear
    to be a classic charm, a built charm, a layer, or
    an interface.'''
    if is_built_charm(asset_path):
        return 'charm (built)'
    elif is_classic_charm(asset_path):
        return 'charm (classic)'
    elif is_source_charm(asset_path):
        return 'charm (source)'
    elif is_charm_layer(asset_path):
        return 'layer'
    elif is_charm_interface(asset_path):
        return 'interface'
    else:
        return None

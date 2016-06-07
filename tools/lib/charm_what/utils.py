#!/usr/bin/python3
'''
charm-what utils
'''

import os


def is_charm_layer(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm layer.'''
    return (
        os.path.isfile(os.path.join(asset_path, 'layer.yaml')) and not
        os.path.isfile(os.path.join(asset_path, '.build.manifest')) and not
        os.path.isfile(os.path.join(asset_path, 'interface.yaml'))
    )


def is_classic_charm(asset_path):
    '''Return True if the contents of asset_path appear to
    be a classic (non-layered) Juju charm.'''
    return (
        os.path.isfile(os.path.join(asset_path, 'metadata.yaml')) and not
        os.path.isfile(os.path.join(asset_path, 'layer.yaml')) and not
        os.path.isfile(os.path.join(asset_path, '.build.manifest')) and not
        os.path.isfile(os.path.join(asset_path, 'interface.yaml'))
    )


def is_charm_interface(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm interface.'''
    return (
        os.path.isfile(os.path.join(asset_path, 'interface.yaml')) and not
        os.path.isfile(os.path.join(asset_path, '.build.manifest')) and not
        os.path.isfile(os.path.join(asset_path, 'layer.yaml'))
    )


def is_built_charm(asset_path):
    '''Return True if the contents of asset_path appear to
    be a Juju charm built from layers and/or interfaces.'''
    return (
        os.path.isfile(os.path.join(asset_path, 'config.yaml')) and
        os.path.isfile(os.path.join(asset_path, 'metadata.yaml')) and
        os.path.isfile(os.path.join(asset_path, 'layer.yaml')) and
        os.path.isfile(os.path.join(asset_path, '.build.manifest')) and not
        os.path.isfile(os.path.join(asset_path, 'interface.yaml'))
    )


def whatis(asset_path):
    '''Attempt to determine if the contents of asset_path appear
    to be a classic charm, a built charm, a layer, or
    an interface.'''
    if is_built_charm(asset_path):
        return 'charm (built)'
    elif is_classic_charm(asset_path):
        return 'charm (classic)'
    elif is_charm_layer(asset_path):
        return 'layer'
    elif is_charm_interface(asset_path):
        return 'interface'
    else:
        return 'unknown'

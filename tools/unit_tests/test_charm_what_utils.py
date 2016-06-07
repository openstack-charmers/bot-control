import os
import unittest
import charm_what.utils as utils

EXPECTED = {
    # sample-test-dir: expected-response
    'built-charm': 'charm (built)',
    'classic-charm': 'charm (classic)',
    'interface': 'interface',
    'layer': 'layer',
    'foobar': 'unknown'
}


class ValidateCharmWhatUtilsTestCase(unittest.TestCase):

    def setUp(self):
        # TODO: Load reference bundle yaml into dict
        self.ref_bundle = None

    def test_whatis_classic_charm(self):
        '''Validate classic charm dir.'''
        asset = 'classic-charm'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  asset)

        result = utils.whatis(asset_path)
        self.assertEqual(EXPECTED[asset], result)

    def test_whatis_built_charm(self):
        '''Validate built charm dir.'''
        asset = 'built-charm'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  asset)

        result = utils.whatis(asset_path)
        self.assertEqual(EXPECTED[asset], result)

    def test_whatis_layer(self):
        '''Validate classic charm dir.'''
        asset = 'layer'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  asset)

        result = utils.whatis(asset_path)
        self.assertEqual(EXPECTED[asset], result)

    def test_whatis_interface(self):
        '''Validate interface dir.'''
        asset = 'interface'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  asset)

        result = utils.whatis(asset_path)
        self.assertEqual(EXPECTED[asset], result)

    def test_negative_whatis_classic_charm(self):
        '''A classic charm is not a built charm.'''
        asset = 'classic-charm'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  'built-charm')

        result = utils.whatis(asset_path)
        self.assertNotEqual(EXPECTED[asset], result)

    def test_negative_whatis_built_charm(self):
        '''A built charm is not a layer.'''
        asset = 'built-charm'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  'layer')

        result = utils.whatis(asset_path)
        self.assertNotEqual(EXPECTED[asset], result)

    def test_negative_whatis_layer(self):
        '''A layer is not a built charm.'''
        asset = 'layer'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  'built-charm')

        result = utils.whatis(asset_path)
        self.assertNotEqual(EXPECTED[asset], result)

    def test_negative_whatis_interface(self):
        '''An interface is not a built charm.'''
        asset = 'interface'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  'built-charm')

        result = utils.whatis(asset_path)
        self.assertNotEqual(EXPECTED[asset], result)

    def test_whatis_foobar(self):
        '''An interface is not a built charm.'''
        asset = 'foobar'
        asset_path = os.path.join('unit_tests',
                                  'charm_what_test_files',
                                  asset)

        result = utils.whatis(asset_path)
        self.assertEqual(EXPECTED[asset], result)

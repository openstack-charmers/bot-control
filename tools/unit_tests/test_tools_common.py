import unittest
import common.tools_common as tools_common


class ValidateCommonToolsTestCase(unittest.TestCase):

    def setUp(self):
        # TODO: Load reference bundle yaml into dict
        self.ref_bundle = None

    def test_get_all_services(self):
        '''
        Validate get all services
        '''
        expected = {'a', 'b', 'c', 'd', 'e', 'f'}

        sample_data = {
            'foo': {
                'services': {
                    'a': {77: [1, 2, 3]},
                    'b': {88: [4, 5, 6]},
                    'c': {99: [7, 8, 9]},
                }
            },
            'bar': {
                'services': {
                    'd': [10, 20, 30],
                    'e': [40, 50, 60],
                    'f': [70, 80, 90],
                }
            }
        }

        result = tools_common.get_all_services(sample_data)
        self.assertSetEqual(expected, result)

    def test_fresh_bundle_dict(self):
        '''
        Validates fresh bundle
        '''
        expected = {
            'services': {},
            'series': None,
            'relations': []
        }
        actual = tools_common.get_fresh_bundle()
        self.assertDictEqual(expected, actual)

    def test_validate_target_exists(self):
        '''
        Validate target exists in bundle dict data
        '''
        sample_data = {
            'foo': 123
        }
        result = tools_common.validate_target_exists(sample_data, 'foo')
        self.assertTrue(result)

    def test_validate_target_exists_not(self):
        '''
        Validates target does not exist in bundle dict data
        '''
        sample_data = {}
        result = tools_common.validate_target_exists(sample_data, 'foo')
        self.assertFalse(result)

    def test_target_inherits(self):
        '''
        Validate target inherits something
        '''
        sample_data = {
            'foo_target': {
                'inherits': 123
                }
        }
        result = tools_common.validate_target_inherits(sample_data,
                                                       'foo_target')
        self.assertTrue(result)

    def test_target_inherits_not(self):
        '''
        Validate target does not inherit something
        '''
        sample_data = {
            'foo_target': {}
        }
        result = tools_common.validate_target_inherits(sample_data,
                                                       'foo_target')
        self.assertFalse(result)

    def test_rm_inheritance_targets(self):
        '''
        Validate remove all inheritance targets
        '''
        expected = {
            'e': {
                'services': {
                    'a': {77: [1, 2, 3]}
                }
            }
        }
        sample_data = {
            'e': {
                'services': {
                    'a': {77: [1, 2, 3]}
                }
            },
            'd': {'inherits': 'e'},
            'c': {'inherits': 'd'},
            'b': {'inherits': 'c'},
            'a': {'inherits': 'b'}
        }

        result = tools_common.rm_inheritance_targets(sample_data)
        self.assertDictEqual(expected, result)

    def test_rm_attr_from_services(self):
        '''
        Validate attribute removal from all services
        '''
        expected = {
            'foo': {
                'services': {
                    'a': {77: [1, 2, 3]},
                    'b': {88: [4, 5, 6]},
                    'c': {99: [7, 8, 9]},
                }
            },
            'bar': {
                'services': {
                    'd': [10, 20, 30],
                    'e': [40, 50, 60],
                    'f': [70, 80, 90],
                }
            }
        }
        sample_data = {
            'foo': {
                'services': {
                    'a': {77: [1, 2, 3]},
                    'b': {88: [4, 5, 6]},
                    'c': {99: [7, 8, 9]},
                }
            },
            'bar': {
                'services': {
                    'd': [10, 20, 30],
                    'e': [40, 50, 60],
                    'f': [70, 80, 90],
                }
            }
        }
        attr = 'constraints'
        result = tools_common.rm_attr_from_services(sample_data, attr)
        self.assertDictEqual(expected, result)

    def test_get_lineage(self):
        '''
        Validate lineage
        '''
        expected = ['a', 'b', 'c', 'd', 'e']

        sample_data = {
            'e': {},
            'd': {'inherits': 'e'},
            'c': {'inherits': 'd'},
            'b': {'inherits': 'c'},
            'a': {'inherits': 'b'}
        }

        result = tools_common.get_lineage(sample_data, 'a')
        self.assertListEqual(expected, result)

    def test_get_lineage_more_complex(self):
        '''
        Validate lineage
        '''

        expected = ['a', 'c', 'e']

        sample_data = {
            'e': {},
            'd': {'foo': 'bar'},
            'c': {'inherits': 'e'},
            'b': {'foo': 'bar'},
            'a': {'inherits': 'c'}
        }

        result = tools_common.get_lineage(sample_data, 'a')
        self.assertListEqual(expected, result)

    def test_get_lineage_no_inheritance(self):
        '''
        Validate lineage with no inheritance
        '''

        expected = ['c']

        sample_data = {
            'e': {'foo': 5},
            'd': {'foo': 4},
            'c': {'foo': 3},
            'b': {'foo': 2},
            'a': {'foo': 1}
        }

        result = tools_common.get_lineage(sample_data, 'c')
        self.assertListEqual(expected, result)

    def test_rm_inheritance_targets_no_inheritance(self):
        '''
        Validate removal of targets when none inherit
        '''

        expected = {
            'e': {'foo': 5},
            'd': {'foo': 4},
            'c': {'foo': 3},
            'b': {'foo': 2},
            'a': {'foo': 1}
        }

        sample_data = {
            'e': {'foo': 5},
            'd': {'foo': 4},
            'c': {'foo': 3},
            'b': {'foo': 2},
            'a': {'foo': 1}
        }

        result = tools_common.rm_inheritance_targets(sample_data)
        self.assertDictEqual(expected, result)

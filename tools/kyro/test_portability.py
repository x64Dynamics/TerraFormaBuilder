"""Portability checks distinguish machine paths from dependency URLs."""
import unittest
from portability import absolute_paths


class PortabilityTest(unittest.TestCase):
    def test_dependency_urls_are_portable(self):
        self.assertFalse(absolute_paths('https://maven.example.org/artifacts'))

    def test_local_installations_are_rejected(self):
        self.assertTrue(absolute_paths('java.home=' + chr(67) + ':' + '/Program Files/Java'))
        self.assertTrue(absolute_paths('java.home=' + '/' + 'home/developer/jdk'))
        self.assertTrue(absolute_paths('root=' + '\\' * 2 + 'workstation' + '\\' + 'share'))


if __name__ == '__main__':
    unittest.main()

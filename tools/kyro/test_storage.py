"""Verify that interrupted inventory persistence preserves the previous valid file."""
from pathlib import Path
import json
import unittest
from unittest.mock import patch
import uuid
from storage import atomic_json, atomic_bytes
from template import ROOT

class AtomicStorageTest(unittest.TestCase):
    def test_another_downloader_can_publish_identical_bytes(self):
        directory = ROOT.parent / (ROOT.name+'-support') / 'tests' / uuid.uuid4().hex
        directory.mkdir(parents=True)
        path = directory / 'object'
        path.write_bytes(b'verified object')
        with patch('storage.os.replace',side_effect=PermissionError('another downloader owns the file')):
            atomic_bytes(path,b'verified object')
        self.assertEqual(b'verified object',path.read_bytes())
        self.assertEqual([],list(directory.glob('*.tmp')))

    def test_failed_replace_preserves_previous_inventory(self):
        directory = ROOT.parent / (ROOT.name+'-support') / 'tests' / uuid.uuid4().hex
        directory.mkdir(parents=True)
        path = directory / 'inventory.json'
        atomic_json(path,{'previous':True})
        with patch('storage.os.replace',side_effect=OSError('simulated write failure')):
            with self.assertRaises(OSError): atomic_json(path,{'replacement':True})
        self.assertEqual({'previous':True},json.loads(path.read_text()))
        self.assertEqual([],list(directory.glob('*.tmp')))

if __name__ == '__main__': unittest.main()

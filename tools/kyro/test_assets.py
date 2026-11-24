"""Concurrent versions must retain independent indexes and immutable shared objects."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import unittest
from unittest.mock import patch
import uuid
from minecraft_cache import seed_assets
from template import ROOT


class AssetCacheTest(unittest.TestCase):
    def setup_cache(self):
        directory = ROOT.parent / (ROOT.name+'-support') / 'tests' / uuid.uuid4().hex
        directory.mkdir(parents=True)
        blob = b'verified shared asset'
        digest = hashlib.sha1(blob).hexdigest()
        responses = {f'https://resources.download.minecraft.net/{digest[:2]}/{digest}':blob}
        metadata = {}
        for version in ('1.20.2','1.20.3'):
            index = json.dumps({'objects':{version+'.ogg':{'hash':digest,'size':len(blob)}}}).encode()
            url = 'https://piston-meta.mojang.com/test/'+version
            responses[url] = index
            path = directory / (version+'.json')
            path.write_text(json.dumps({'id':version,'assetIndex':{'id':version,'url':url,'sha1':hashlib.sha1(index).hexdigest()}}))
            metadata[version] = path
        return directory, blob, digest, responses, metadata

    def test_concurrent_versions_keep_both_indexes(self):
        directory, blob, digest, responses, metadata = self.setup_cache()
        def prepare(version):
            seed_assets(version,2,metadata[version],'template-'+version)
        with patch('minecraft_cache.cache_home',return_value=directory), patch('minecraft_cache.fetch',side_effect=responses.__getitem__):
            with ThreadPoolExecutor(max_workers=2) as pool:
                list(pool.map(prepare,metadata))
        assets = directory / 'caches/minecraft/assets'
        for version in metadata:
            index = json.loads((assets / 'indexes' / ('template-'+version+'.json')).read_text())
            self.assertIn(version+'.ogg',index['objects'])
        self.assertEqual(blob,(assets / 'objects' / digest[:2] / digest).read_bytes())
        self.assertEqual([],list(assets.rglob('*.tmp')))

    def test_offline_never_fetches_missing_assets(self):
        directory, _, _, _, metadata = self.setup_cache()
        with patch('minecraft_cache.cache_home',return_value=directory), patch('minecraft_cache.fetch') as fetch:
            with self.assertRaises(ValueError):
                seed_assets('1.20.2',2,metadata['1.20.2'],'template-1.20.2',offline=True)
            fetch.assert_not_called()


if __name__ == '__main__':
    unittest.main()

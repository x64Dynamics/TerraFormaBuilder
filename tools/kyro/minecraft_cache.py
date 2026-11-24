"""Seed ForgeGradle 6's library cache from checksum-verified Mojang metadata."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import hashlib
import json
import re
from catalog import fetch
from storage import cache_home, atomic_bytes


def seed_assets(version, workers=8, metadata_file=None, index_name=None, offline=False, asset_root=None, index_url=None):
    home = cache_home()
    cached = home / 'caches/fabric-loom' / version / 'mojang_minecraft_info.json'
    if index_url:
        match = re.fullmatch(r'https://piston-meta\.mojang\.com/v1/packages/([0-9a-f]{40})/([A-Za-z0-9_.-]+)\.json',index_url)
        if not match:
            raise ValueError('Asset index URL must identify a checksum-addressed Mojang index')
        metadata = {'id':version,'assetIndex':{'id':match[2],'url':index_url,'sha1':match[1]}}
    elif metadata_file:
        metadata = json.loads(Path(metadata_file).read_text())
    elif cached.is_file():
        metadata = json.loads(cached.read_text())
    else:
        if offline:
            raise ValueError('Minecraft metadata is not cached for offline asset preparation')
        manifest = json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        release = next(item for item in manifest['versions'] if item['id'] == version)
        metadata = json.loads(fetch(release['url']))
    index = metadata['assetIndex']
    assert metadata['id'] == version, 'Minecraft metadata version mismatch'
    index_name = index_name or index['id']
    assert re.fullmatch(r'[A-Za-z0-9_.-]+', index_name), 'Invalid asset index name'
    target = Path(asset_root).resolve() if asset_root else home / 'caches/minecraft/assets'
    index_file = target / 'indexes' / (index_name+'.json')
    data = index_file.read_bytes() if index_file.is_file() else b''
    if hashlib.sha1(data).hexdigest() != index['sha1']:
        if offline:
            raise ValueError('The required asset index is not cached for offline use')
        data = fetch(index['url'])
    assert hashlib.sha1(data).hexdigest() == index['sha1'], 'Asset index checksum mismatch'
    index_file.parent.mkdir(parents=True,exist_ok=True)
    atomic_bytes(index_file, data)
    objects = {item['hash']:item for item in json.loads(data)['objects'].values()}
    roots = [target] + [home / 'caches' / folder / 'assets' for folder in ('minecraft','fabric-loom','forge_gradle','neoformruntime')]
    def copy_object(item):
        digest = item['hash']
        assert re.fullmatch('[0-9a-f]{40}', digest), 'Invalid asset object hash'
        relative = Path('objects') / digest[:2] / digest
        destination = target / relative
        for root in roots:
            source = root / relative
            if source.is_file() and source.stat().st_size == item['size']:
                content = source.read_bytes()
                if hashlib.sha1(content).hexdigest() == digest:
                    if source == destination:
                        return 0
                    break
        else:
            if offline:
                raise ValueError(f'Asset {digest} is not cached for offline use')
            content = fetch(f'https://resources.download.minecraft.net/{digest[:2]}/{digest}')
        assert len(content) == item['size'] and hashlib.sha1(content).hexdigest() == digest, 'Asset checksum mismatch'
        destination.parent.mkdir(parents=True, exist_ok=True)
        atomic_bytes(destination, content)
        return 1
    with ThreadPoolExecutor(max_workers=workers) as pool:
        copied = sum(pool.map(copy_object, objects.values()))
    print(f'Validated {len(objects)} assets for {version}; seeded {copied} into the native Minecraft asset cache.')

def seed(version, workers=8):
    manifest = json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    release = next(v for v in manifest['versions'] if v['id']==version and v['type']=='release')
    metadata = json.loads(fetch(release['url']))
    artifacts = {}
    for library in metadata['libraries']:
        downloads = library.get('downloads',{})
        for artifact in [downloads.get('artifact'),*downloads.get('classifiers',{}).values()]:
            if artifact: artifacts[artifact['path']] = artifact
    directory = cache_home() / 'caches/forge_gradle/maven_downloader'
    directory.mkdir(parents=True,exist_ok=True)
    directory = directory.resolve()

    def download(artifact):
        destination = (directory / artifact['path']).resolve()
        if not destination.is_relative_to(directory): raise ValueError('Library path escapes cache')
        destination.parent.mkdir(parents=True,exist_ok=True)
        valid = destination.is_file() and hashlib.sha1(destination.read_bytes()).hexdigest()==artifact['sha1']
        if not valid:
            data = fetch(artifact['url'])
            if len(data)!=artifact['size'] or hashlib.sha1(data).hexdigest()!=artifact['sha1']:
                raise ValueError(f'Library verification failed: {artifact["path"]}')
            atomic_bytes(destination, data)
        # ForgeGradle uses a local MD5 sidecar to avoid probing every Maven mirror.
        destination.with_name(destination.name+'.md5').write_text(hashlib.md5(destination.read_bytes()).hexdigest(),encoding='ascii')
        return not valid

    with ThreadPoolExecutor(max_workers=workers) as pool:
        downloaded = sum(pool.map(download,artifacts.values()))
    print(f'Validated {len(artifacts)} Mojang libraries for {version}; downloaded {downloaded}.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('version')
    parser.add_argument('--workers',type=int,default=8,choices=range(1,17))
    parser.add_argument('--assets',action='store_true',help='Seed NeoGradle assets, reusing verified objects from other native loader caches')
    parser.add_argument('--metadata',type=Path,help='Native Minecraft version JSON to use for asset preparation')
    parser.add_argument('--index-url',help='Exact checksum-addressed Mojang index URL reported by a native launcher')
    parser.add_argument('--asset-root',type=Path,help='Native asset-cache directory; defaults to the NeoGradle cache')
    parser.add_argument('--asset-index-name',help='Version-specific asset index filename without its extension')
    parser.add_argument('--offline',action='store_true')
    args = parser.parse_args()
    if args.assets:
        seed_assets(args.version,args.workers,args.metadata,args.asset_index_name,args.offline,args.asset_root,args.index_url)
    else:
        seed(args.version,args.workers)

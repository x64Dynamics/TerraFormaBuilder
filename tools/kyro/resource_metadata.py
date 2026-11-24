"""Seed version-specific pack metadata from Minecraft's own cached version information."""
import json
import zipfile
from template import ROOT, inventory
from storage import cache_home


def pack_metadata(version):
    pack = {'description':'Mod resources'}
    if isinstance(version, int):
        pack['pack_format'] = version
    elif 'resource_major' in version:
        pack['min_format'] = [version['resource_major'], version['resource_minor']]
        pack['max_format'] = [version['resource_major'], version['resource_minor']]
    else:
        pack['pack_format'] = version['resource']
    return {'pack':pack}


def main():
    count = 0
    for target in inventory():
        mc = target['minecraft']
        destination = ROOT / 'versions' / mc / 'common/src/main/resources/pack.mcmeta'
        if destination.exists():
            continue
        jar = cache_home() / 'caches/fabric-loom' / mc / 'minecraft-client.jar'
        with zipfile.ZipFile(jar) as archive:
            version = json.loads(archive.read('version.json'))
        assert version['id'] == mc, 'Cached Minecraft version does not match target'
        destination.write_text(json.dumps(pack_metadata(version['pack_version']), indent=2)+'\n', encoding='utf-8')
        count += 1
    print(f'Seeded {count} version-specific resource pack metadata files.')


if __name__ == '__main__':
    main()

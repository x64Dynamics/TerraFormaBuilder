"""Build a fresh source-only copy without transferring local settings or caches."""
from pathlib import Path
import argparse
import os
import shutil
import subprocess
import sys
import uuid
from template import ROOT, properties
from storage import cache_home


def verify(destination_root, target=None):
    destination = destination_root.resolve() / ('relocated template ' + uuid.uuid4().hex[:12])
    if destination.is_relative_to(ROOT.resolve()):
        raise ValueError('Relocation checks must stay outside the template repository')
    destination.mkdir(parents=True, exist_ok=False)
    names = subprocess.check_output(['git','ls-files','-z'], cwd=ROOT).decode().split('\0')
    for relative in filter(None, names):
        source = ROOT / relative
        if not source.resolve().is_relative_to(ROOT.resolve()):
            raise ValueError('Source file escapes repository root')
        output = destination / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, output)
    assert not (destination / 'local.properties').exists(), 'Local settings must never travel with template sources'
    env = dict(os.environ)
    env['GRADLE_USER_HOME'] = str(cache_home())
    local = properties(ROOT / 'local.properties') if (ROOT / 'local.properties').is_file() else {}
    for major in (8,16,17,21,25):
        value = local.get(f'java.{major}.home')
        if value:
            env[f'JAVA_{major}_HOME'] = value
    helper = destination / 'tools/kyro/template.py'
    subprocess.run([sys.executable, str(helper), 'validate'], cwd=destination, env=env, check=True)
    arguments = ['core']
    if target:
        mc, loader = target.split('/')
        arguments = ['gradle', f'-Pmc={mc}', f'-Ploader={loader}', 'build']
    subprocess.run([sys.executable, str(helper), *arguments], cwd=destination, env=env, check=True)
    print(f'PASS: source-only relocation build in {destination}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--destination-root', type=Path, required=True)
    parser.add_argument('--target', help='Optional minecraft/loader; defaults to the standalone shared-core build')
    args = parser.parse_args()
    verify(args.destination_root, args.target)

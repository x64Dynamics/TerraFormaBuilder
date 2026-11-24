"""Atomic JSON persistence and matrix storage preflight checks."""
from pathlib import Path
import json
import os
import shutil
import time
import uuid


def atomic_bytes(path, data):
    """Publish a verified cache object, tolerating another downloader's file lock."""
    path = Path(path)
    temporary = path.with_name(path.name+'.'+uuid.uuid4().hex+'.tmp')
    try:
        with temporary.open('xb') as output:
            output.write(data)
        for attempt in range(30):
            try:
                os.replace(temporary, path)
                return
            except PermissionError:
                try:
                    if path.read_bytes() == data:
                        return
                except OSError:
                    pass
                if attempt == 29:
                    raise
                time.sleep(0.2)
    finally:
        if temporary.is_file():
            assert temporary.parent.resolve() == path.parent.resolve()
            temporary.unlink()

def atomic_json(path, value):
    path = Path(path)
    temporary = path.with_name(path.name+'.'+uuid.uuid4().hex+'.tmp')
    try:
        with temporary.open('x',encoding='utf-8',newline='\n') as output:
            json.dump(value,output,indent=2)
            output.write('\n')
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary,path)
    finally:
        if temporary.is_file():
            assert temporary.parent.resolve() == path.parent.resolve()
            temporary.unlink()

def cache_home():
    home = os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle'))
    local = Path(__file__).resolve().parents[2] / 'local.properties'
    if local.is_file():
        for line in local.read_text(encoding='utf-8').splitlines():
            if line.startswith('gradle.user.home='):
                home = line.partition('=')[2].strip()
    return Path(home).resolve()

def require_space(target_count, cached_count=0):
    home = cache_home()
    home.mkdir(parents=True,exist_ok=True)
    required = (2 + (target_count-cached_count)*0.5 + cached_count*0.05)*1024**3
    free = shutil.disk_usage(home).free
    if free < required:
        raise ValueError(f'Insufficient build-cache space: {free/1024**3:.1f} GiB free, estimated {required/1024**3:.1f} GiB needed. Set GRADLE_USER_HOME to a drive with enough space.')

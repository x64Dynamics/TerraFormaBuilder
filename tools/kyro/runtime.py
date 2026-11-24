"""Smoke-test a native dedicated-server run task in a fresh loopback-only world."""
from datetime import datetime, timezone
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import json
import os
import secrets
import signal
import socket
import struct
import subprocess
import sys
import time
import uuid
from template import ROOT, selected, properties, inventory
from storage import atomic_json
from resources import worker_budget


def terminate_test(process):
    if process.poll() is None:
        if sys.platform == 'win32':
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
        else:
            os.killpg(process.pid, signal.SIGTERM)
        process.wait(timeout=30)


def start_test(command, output):
    return subprocess.Popen(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT,
                            stdin=subprocess.PIPE, start_new_session=sys.platform != 'win32')


def client(mc, loader, run_root, reports, timeout):
    selected([f'-Pmc={mc}', f'-Ploader={loader}'])
    identifier = uuid.uuid4().hex[:12]
    isolated = run_root.resolve() / identifier
    game = isolated / mc / loader / 'client'
    game.mkdir(parents=True, exist_ok=False)
    (game / 'options.txt').write_text('maxFps:30\nrenderDistance:2\nsoundCategory_master:0.0\n', encoding='utf-8')
    reports.mkdir(parents=True, exist_ok=True)
    log = reports / f'client-{mc}-{loader}-{identifier}.log'
    metadata = properties(ROOT / 'properties/mod.properties')
    result = {'minecraft':mc, 'loader':loader, 'mode':'client', 'log':str(log),
              'game_directory':str(game), 'checked_at':datetime.now(timezone.utc).isoformat(), 'status':'failed'}
    command = [sys.executable, str(ROOT / 'tools/kyro/template.py'), 'gradle',
               f'-Pmc={mc}', f'-Ploader={loader}', f'-PrunRoot={isolated}', '-PsmokeTest=true', 'runClient']
    with log.open('w', encoding='utf-8') as output:
        process = start_test(command, output)
        ready_since = None
        try:
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline and process.poll() is None:
                text = log.read_text(encoding='utf-8', errors='replace')
                fatal = ('Reported exception thrown!', 'Failed to complete lifecycle event',
                         'Mod Loading has failed', 'Failed to create mod instance',
                         'Error during mod loading', 'ModLoadingException',
                         'Encountered an unexpected exception')
                assert not any(marker in text for marker in fatal), 'Fatal client loading error in log'
                initialized = f'{metadata["mod.name"]} initialized on {loader}' in text
                audio = 'OpenAL initialized' in text or 'Sound engine started' in text
                textures = 'Created:' in text and 'atlas' in text
                if initialized and audio and textures:
                    ready_since = ready_since or time.monotonic()
                    if time.monotonic() - ready_since >= 15:
                        config = game / 'config' / (metadata['mod.id'] + '.json')
                        assert json.loads(config.read_text())['enabled'] is True
                        result.update(status='client-startup-verified', evidence='Shared initializer, JSON configuration, audio and texture atlases; process remained alive for 15 seconds after resource creation.')
                        break
                time.sleep(1)
            if result['status'] == 'failed':
                result['error'] = 'Client exited before resource-ready state' if process.poll() is not None else 'Timed out waiting for resource-ready state'
        except (AssertionError, OSError, ValueError) as error:
            result['error'] = str(error)
        finally:
            terminate_test(process)
    atomic_json(reports / f'client-{mc}-{loader}-{identifier}.json', result)
    print(json.dumps(result), flush=True)
    return result['status'] == 'client-startup-verified'


def receive(connection, count):
    data = b''
    while len(data) < count:
        chunk = connection.recv(count - len(data))
        if not chunk:
            raise ConnectionError('RCON closed unexpectedly')
        data += chunk
    return data


def packet(connection, identifier, kind, body):
    payload = struct.pack('<ii', identifier, kind) + body.encode() + b'\0\0'
    connection.sendall(struct.pack('<i', len(payload)) + payload)


def response(connection):
    size = struct.unpack('<i', receive(connection, 4))[0]
    if not 10 <= size <= 1048576:
        raise ValueError('Invalid RCON response length')
    payload = receive(connection, size)
    return struct.unpack('<ii', payload[:8])


def stop_server(port, password):
    with socket.create_connection(('127.0.0.1', port), timeout=10) as connection:
        packet(connection, 1, 3, password)
        for _ in range(3):
            identifier, kind = response(connection)
            if identifier == -1:
                raise ValueError('RCON authentication failed')
            if identifier == 1 and kind == 2:
                packet(connection, 2, 2, 'stop')
                return
        raise ValueError('RCON authentication response missing')


def free_port():
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        return listener.getsockname()[1]


def server(mc, loader, run_root, reports, timeout, stop_method='rcon'):
    selected([f'-Pmc={mc}', f'-Ploader={loader}'])
    identifier = uuid.uuid4().hex[:12]
    isolated = run_root.resolve() / identifier
    game = isolated / mc / loader / 'server'
    game.mkdir(parents=True, exist_ok=False)
    port, rcon = free_port(), free_port()
    while port == rcon:
        rcon = free_port()
    password = secrets.token_hex(24)
    (game / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    settings = {'server-ip':'127.0.0.1', 'server-port':port, 'online-mode':'false',
                'enable-rcon':'true', 'rcon.port':rcon, 'rcon.password':password,
                'view-distance':2, 'simulation-distance':2, 'max-players':1,
                'level-name':'smoke-world', 'motd':'Local template smoke test',
                'level-type':'flat', 'generate-structures':'false',
                'broadcast-rcon-to-ops':'false'}
    (game / 'server.properties').write_text(''.join(f'{k}={v}\n' for k,v in settings.items()), encoding='utf-8')
    reports.mkdir(parents=True, exist_ok=True)
    log = reports / f'{mc}-{loader}-{identifier}.log'
    result = {'minecraft':mc, 'loader':loader, 'stop_method':stop_method, 'game_directory':str(game), 'log':str(log),
              'checked_at':datetime.now(timezone.utc).isoformat(), 'status':'failed'}
    command = [sys.executable, str(ROOT / 'tools/kyro/template.py'), 'gradle',
               f'-Pmc={mc}', f'-Ploader={loader}', f'-PrunRoot={isolated}', '-PsmokeTest=true', 'runServer']
    with log.open('w', encoding='utf-8') as output:
        process = start_test(command, output)
        deadline = time.monotonic() + timeout
        started = False
        try:
            while time.monotonic() < deadline and process.poll() is None:
                text = log.read_text(encoding='utf-8', errors='replace')
                if 'Done (' in text and 'For help' in text:
                    started = True
                    if stop_method == 'console':
                        process.stdin.write(b'stop\n')
                        process.stdin.flush()
                        process.stdin.close()
                        try:
                            process.wait(timeout=30)
                        except subprocess.TimeoutExpired:
                            stop_server(rcon, password)
                            process.wait(timeout=90)
                            raise AssertionError('Console stop was not forwarded; RCON fallback stopped the test server')
                        break
                    # The ready log can precede the RCON listener by a few seconds.
                    for attempt in range(15):
                        try:
                            stop_server(rcon, password)
                            process.stdin.close()
                            break
                        except (OSError, ConnectionError):
                            if attempt == 14:
                                raise
                            time.sleep(1)
                    process.wait(timeout=90)
                    break
                time.sleep(1)
            if process.poll() is None:
                raise TimeoutError(f'Server did not finish within {timeout} seconds')
            text = log.read_text(encoding='utf-8', errors='replace')
            metadata = properties(ROOT / 'properties/mod.properties')
            config = game / 'config' / (metadata['mod.id'] + '.json')
            assert started, 'Server never reached world-ready state'
            assert process.returncode == 0, f'Launch exited {process.returncode}'
            assert f'{metadata["mod.name"]} initialized on {loader}' in text, 'Shared initializer was not executed'
            assert json.loads(config.read_text())['enabled'] is True, 'Shared JSON config missing'
            assert 'Stopping server' in text, 'Graceful shutdown not observed'
            result.update(status='runtime-verified', exit_code=process.returncode)
        except (AssertionError, OSError, TimeoutError, ValueError, subprocess.TimeoutExpired) as error:
            result['error'] = str(error)
        finally:
            terminate_test(process)
    atomic_json(reports / f'{mc}-{loader}-{identifier}.json', result)
    print(json.dumps(result), flush=True)
    return result['status'] == 'runtime-verified'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument('--target', action='append', help='minecraft/loader; repeat for parallel checks')
    selection.add_argument('--all', action='store_true', help='Exercise every catalogued target')
    parser.add_argument('--resume', action='store_true', help='Skip successful runtime records for the selected mode')
    parser.add_argument('--mode', choices=('client','server'), default='server')
    parser.add_argument('--stop-method', choices=('rcon','console'), default='rcon')
    parser.add_argument('--workers', type=int, choices=range(1,5), default=1)
    parser.add_argument('--run-root', type=Path, required=True)
    parser.add_argument('--report-dir', type=Path, required=True)
    parser.add_argument('--timeout', type=int, default=900)
    parser.add_argument('--accept-eula', action='store_true', help='Confirm that YOU accept the Minecraft EULA for this test')
    args = parser.parse_args()
    if args.mode == 'server' and not args.accept_eula:
        parser.error('Read https://aka.ms/MinecraftEULA and accept it before using --accept-eula.')
    if args.run_root.resolve().is_relative_to(ROOT.resolve()) or args.report_dir.resolve().is_relative_to(ROOT.resolve()):
        parser.error('Use external run and report directories for smoke tests.')
    targets = [(item['minecraft'], loader) for item in inventory() for loader in item['loaders']] if args.all else [tuple(target.split('/')) for target in args.target]
    if args.resume:
        successful = set()
        for path in args.report_dir.glob('*.json'):
            record = json.loads(path.read_text())
            mode = record.get('mode', 'server')
            if mode == args.mode and record['status'] in ('runtime-verified','client-startup-verified'):
                successful.add((record['minecraft'],record['loader']))
        targets = [target for target in targets if target not in successful]
    action = client if args.mode == 'client' else server
    def run(target):
        if args.mode == 'server':
            return server(*target, args.run_root, args.report_dir, args.timeout, args.stop_method)
        return action(*target, args.run_root, args.report_dir, args.timeout)
    with ThreadPoolExecutor(max_workers=worker_budget(args.workers, per_worker_gib=2)) as executor:
        results = list(executor.map(run, targets))
    return 0 if all(results) else 1


if __name__ == '__main__':
    sys.exit(main())

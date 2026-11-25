"""Synchronize verified client renderer, events, and block scanner across NeoForge targets."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC_RENDERER = ROOT / 'versions/1.21.1/neoforge/src/main/java/com/x64Dynamics/tfb/client/TFBClientRenderer.java'
SRC_EVENTS = ROOT / 'versions/1.21.1/neoforge/src/main/java/com/x64Dynamics/tfb/client/TFBClientEvents.java'
SRC_SCANNER = ROOT / 'versions/1.21.1/neoforge/src/main/java/com/x64Dynamics/tfb/BlockRegistryScanner.java'
SRC_ENTRYPOINT = ROOT / 'versions/1.21.1/neoforge/src/main/java/com/x64Dynamics/tfb/Entrypoint.java'

TARGET_VERSIONS = ['1.21', '1.20.6', '1.20.5']

def sync():
    renderer_content = SRC_RENDERER.read_text(encoding='utf-8')
    events_content = SRC_EVENTS.read_text(encoding='utf-8')
    scanner_content = SRC_SCANNER.read_text(encoding='utf-8')
    entry_content = SRC_ENTRYPOINT.read_text(encoding='utf-8')

    for v in TARGET_VERSIONS:
        dest_renderer = ROOT / f'versions/{v}/neoforge/src/main/java/com/x64Dynamics/tfb/client/TFBClientRenderer.java'
        dest_events = ROOT / f'versions/{v}/neoforge/src/main/java/com/x64Dynamics/tfb/client/TFBClientEvents.java'
        dest_scanner = ROOT / f'versions/{v}/neoforge/src/main/java/com/x64Dynamics/tfb/BlockRegistryScanner.java'
        dest_entry = ROOT / f'versions/{v}/neoforge/src/main/java/com/x64Dynamics/tfb/Entrypoint.java'

        if dest_renderer.parent.exists():
            dest_renderer.write_text(renderer_content, encoding='utf-8', newline='\n')
            dest_events.write_text(events_content, encoding='utf-8', newline='\n')
            dest_scanner.write_text(scanner_content, encoding='utf-8', newline='\n')
            dest_entry.write_text(entry_content, encoding='utf-8', newline='\n')
            print(f'Synchronized {v}/neoforge client renderer, events, scanner, and entrypoint.')

if __name__ == '__main__':
    sync()

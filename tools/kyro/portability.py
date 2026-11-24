"""Reject workstation-specific paths in the distributable template."""
from pathlib import Path
import re
import subprocess
from template import ROOT


def absolute_paths(text):
    # Requiring a word boundary avoids mistaking https:// URLs for drive paths.
    prefixes = '|'.join(re.escape('/' + name + '/') for name in ('Users', 'home'))
    return re.findall(r'(?<![\w])(?:[A-Za-z]:[\\/]|' + prefixes + r'|\\\\[\w.-]+\\)', text)


def check():
    names = subprocess.check_output(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT
    ).decode().split('\0')
    findings = []
    for relative in sorted(set(filter(None, names))):
        path = ROOT / relative
        if not path.is_file():
            continue
        try:
            content = path.read_text(encoding='utf-8')
        except UnicodeDecodeError:
            continue
        for number, line in enumerate(content.splitlines(), 1):
            if absolute_paths(line):
                findings.append(f'{relative}:{number}: workstation path')
    if findings:
        raise ValueError('\n'.join(findings))
    print('PASS: no workstation-specific absolute paths in template files.')


if __name__ == '__main__':
    check()

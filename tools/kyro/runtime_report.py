"""Summarize native startup evidence without exposing local workstation paths."""
from pathlib import Path
import argparse
import json
from template import ROOT, inventory
from storage import atomic_json


def summarize(reports, builds):
    latest = {}
    for path in reports.glob('*.json'):
        record = json.loads(path.read_text(encoding='utf-8'))
        key = (record['minecraft'], record['loader'], record.get('mode','server'))
        if key not in latest or record['checked_at'] > latest[key]['checked_at']:
            latest[key] = record
    results = []
    for target in inventory():
        for loader in target['loaders']:
            mc = target['minecraft']
            item = {'minecraft':mc, 'loader':loader, 'java':target['java']}
            build_path = builds / f'{mc}-{loader}.json'
            build = json.loads(build_path.read_text()) if build_path.is_file() else {}
            item['build'] = build.get('status','pending')
            if build.get('sha256'):
                item['jar_sha256'] = build['sha256']
            for mode in ('client','server'):
                record = latest.get((mc,loader,mode), {})
                item[mode] = record.get('status','pending')
                if record.get('checked_at'):
                    item[mode+'_checked_at'] = record['checked_at']
            results.append(item)
    counts = {'targets':len(results),
              'builds_passed':sum(item['build']=='build-verified' for item in results),
              'clients_passed':sum(item['client']=='client-startup-verified' for item in results),
              'servers_passed':sum(item['server']=='runtime-verified' for item in results)}
    complete = all(counts[name] == counts['targets'] for name in ('builds_passed','clients_passed','servers_passed'))
    return {'complete':complete, 'counts':counts,
            'scope':'Development launch tasks: shared initialization and JSON config, client audio/texture startup, dedicated world readiness and clean server shutdown. Visual correctness, gameplay features, and installed production-loader tests are separate checks.',
            'results':results}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report-dir', type=Path, required=True)
    parser.add_argument('--build-report-dir', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--require-complete', action='store_true')
    args = parser.parse_args()
    result = summarize(args.report_dir, args.build_report_dir)
    print(json.dumps(result['counts']))
    if args.output:
        if args.output.resolve().is_relative_to(ROOT.resolve()):
            parser.error('Keep verification reports outside the template repository.')
        args.output.parent.mkdir(parents=True, exist_ok=True)
        atomic_json(args.output, result)
    if args.require_complete and not result['complete']:
        raise SystemExit(1)

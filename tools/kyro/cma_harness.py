"""Automated CMA + TFB verification harness for 1.18.2, 26.x, and core suites."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CMA_ROOT = Path(os.environ.get("CMA_ROOT", str(ROOT.parent / "CMA")))


def run_cmd(cmd: list[str], cwd: Path = ROOT, env: dict[str, str] | None = None, capture: bool = False) -> tuple[int, str]:
    merged = dict(os.environ)
    if env:
        merged.update(env)
    proc = subprocess.run(cmd, cwd=cwd, env=merged, capture_output=capture, text=True, timeout=600)
    return proc.returncode, ((proc.stdout or "") + (proc.stderr or "")) if capture else ""


def discover_cma_instances() -> list[dict]:
    script = CMA_ROOT / "scripts" / "cma-instance.ps1"
    if not script.is_file():
        return []
    code, out = run_cmd(
        ["pwsh", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(script), "list"],
        cwd=CMA_ROOT,
        env={"CMA_CODEX_WORKSPACES_ROOT": os.environ.get("CMA_CODEX_WORKSPACES_ROOT", str(ROOT.parent / "Codex Workspaces"))},
        capture=True,
    )
    if code != 0:
        return []
    try:
        return json.loads(out).get("instances", [])
    except json.JSONDecodeError:
        return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--targets", nargs="+", default=["1.18.2/fabric", "1.18.2/forge", "26.3/fabric", "26.3/neoforge"])
    parser.add_argument("--smoke-client", default="1.18.2/fabric", help="Target for headless/client runtime smoke test")
    parser.add_argument("--skip-smoke", action="store_true")
    args = parser.parse_args()

    print("[1/4] Running common core build + JUnit tests...")
    code, out = run_cmd([sys.executable, str(ROOT / "tools/kyro/template.py"), "core"])
    if code != 0:
        print(out)
        return code

    print(f"[2/4] Verifying target builds: {', '.join(args.targets)}")
    verify_cmd = [sys.executable, str(ROOT / "tools/kyro/verify.py"), "--workers", "2", "--no-catalog-update"]
    for t in args.targets:
        verify_cmd.extend(["--target", t])
    code, out = run_cmd(verify_cmd)
    print(out)
    if code != 0:
        return code

    print("[3/4] Checking CMA Prism instances...")
    instances = discover_cma_instances()
    print(f"CMA instances discovered: {len(instances)}")

    if not args.skip_smoke and args.smoke_client:
        print(f"[4/4] Running runtime client smoke test on {args.smoke_client}...")
        code, out = run_cmd([
            sys.executable, str(ROOT / "tools/kyro/runtime.py"),
            "--target", args.smoke_client,
            "--mode", "client",
            "--run-root", str(ROOT.parent / f"{ROOT.name}-support" / "smoke-runs"),
            "--report-dir", str(ROOT.parent / f"{ROOT.name}-support" / "smoke-reports"),
            "--timeout", "180",
        ])
        print(out)
        return code
    return 0


if __name__ == "__main__":
    sys.exit(main())

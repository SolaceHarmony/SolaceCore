#!/usr/bin/env python3
"""Mirror selected authoritative in-repo documents into wiki pages.

This script is retained for compatibility with older publishing workflows.
The authoritative documentation has moved into ``wiki/`` directly, so there
are currently no source documents to mirror from ``docs/``.

The mapping is an explicit manifest (``MANIFEST`` below) so nothing is mirrored
by accident and hand-authored pages are never clobbered. Each mirrored page
gets a provenance header pointing back at its source.

Run from CI or locally:

    python tools/wiki/sync_pages.py --repo-root .
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

# (source path relative to repo root, target wiki page filename, topic, title)
MANIFEST: list[tuple[str, str, str, str]] = []


def provenance(source_rel: str) -> str:
    return (
        f"<!-- AUTO-SYNCED from {source_rel} by tools/wiki/sync_pages.py. "
        f"Edit the source, not this page. -->\n"
    )


def mirror(repo_root: Path, source_rel: str, target_name: str, topic: str, title: str) -> bool:
    source = repo_root / source_rel
    target = repo_root / "wiki" / target_name
    if not source.is_file():
        print(f"  skip: source missing: {source_rel}")
        return False

    src_text = source.read_text(encoding="utf-8")
    # Strip a leading H1 from the source so we control the page title/topic.
    lines = src_text.splitlines()
    if lines and lines[0].startswith("# "):
        lines = lines[1:]
    body = "\n".join(line.rstrip() for line in lines).lstrip("\n")

    page = (
        f"<!-- topic: {topic} -->\n"
        f"{provenance(source_rel)}\n"
        f"# {title}\n\n"
        f"> Mirrored from `{source_rel}`. This page is regenerated on publish; "
        f"edit the source document.\n\n"
        f"{body}\n"
    ).rstrip() + "\n"
    target.write_text(page, encoding="utf-8")
    print(f"  synced: {source_rel} -> wiki/{target_name}")
    return True


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Mirror source docs into wiki pages")
    ap.add_argument("--repo-root", default=".", help="Repository root")
    args = ap.parse_args(argv)

    repo_root = Path(args.repo_root).resolve()
    (repo_root / "wiki").mkdir(exist_ok=True)

    print("Syncing authoritative docs into wiki pages:")
    count = 0
    for source_rel, target_name, topic, title in MANIFEST:
        if mirror(repo_root, source_rel, target_name, topic, title):
            count += 1
    print(f"Synced {count}/{len(MANIFEST)} page(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

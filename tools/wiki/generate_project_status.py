#!/usr/bin/env python3
"""Generate the consolidated ``wiki/Project-Status.md`` page.

Aggregates the in-repo status documents under ``docs/status/`` into one
wiki page, and optionally overlays live data from a GitHub Projects (v2)
board. The board overlay is **best-effort**: when ``PROJECTS_READ_TOKEN`` is
absent (or the ``requests`` library is unavailable), the generator emits an
explanatory notice rather than failing the build — so the publish workflow is
always green.

Environment (all optional except where noted):
  PROJECTS_READ_TOKEN  Fine-grained PAT / App token with read:project.
  PROJECT_ORG          GitHub org that owns the project board.
  PROJECT_NUMBER       Project number within the org.
  GITHUB_REPOSITORY    "owner/repo" (used only for display).

Run from CI or locally:

    python tools/wiki/generate_project_status.py --repo-root .
"""
from __future__ import annotations

import argparse
import datetime as _dt
import os
import sys
from pathlib import Path

# Status docs to aggregate, in display order. Missing files are skipped.
STATUS_SOURCES = [
    "docs/status/QUICK_STATUS.md",
    "docs/status/PROJECT_STATUS.md",
    "docs/status/DESIGN_VS_IMPLEMENTATION.md",
]


def _demote_headings(text: str, by: int = 1) -> str:
    """Push every Markdown heading down ``by`` levels so embedded docs nest
    under the page's own section headings without creating multiple H1s."""
    out = []
    for line in text.splitlines():
        if line.startswith("#"):
            hashes = len(line) - len(line.lstrip("#"))
            if 1 <= hashes <= 5:
                line = "#" * min(hashes + by, 6) + line[hashes:]
        out.append(line)
    return "\n".join(out)


def aggregate_status(repo_root: Path) -> str:
    parts: list[str] = []
    for rel in STATUS_SOURCES:
        p = repo_root / rel
        if not p.is_file():
            continue
        body = _demote_headings(p.read_text(encoding="utf-8"), by=1)
        parts.append(f"## From `{rel}`\n\n{body.strip()}\n")
    if not parts:
        return "_No status documents found under `docs/status/`._\n"
    return "\n---\n\n".join(parts)


def project_board_section() -> str:
    token = os.environ.get("PROJECTS_READ_TOKEN")
    org = os.environ.get("PROJECT_ORG")
    number = os.environ.get("PROJECT_NUMBER")

    if not token:
        return (
            "> **Live project board:** not configured. Set the "
            "`PROJECTS_READ_TOKEN` secret (read:project) plus `PROJECT_ORG` / "
            "`PROJECT_NUMBER` to overlay live board data here. Until then this "
            "page reflects the in-repo status docs only.\n"
        )
    if not (org and number):
        return (
            "> **Live project board:** `PROJECTS_READ_TOKEN` is set but "
            "`PROJECT_ORG` / `PROJECT_NUMBER` are not. Skipping board overlay.\n"
        )

    try:
        import json
        import urllib.request

        query = """
        query($org:String!, $number:Int!){
          organization(login:$org){
            projectV2(number:$number){
              title
              url
              items(first:100){
                nodes{
                  content{
                    ... on Issue { title state url }
                    ... on PullRequest { title state url }
                  }
                  fieldValues(first:20){
                    nodes{
                      ... on ProjectV2ItemFieldSingleSelectValue { name field { ... on ProjectV2FieldCommon { name } } }
                    }
                  }
                }
              }
            }
          }
        }
        """
        payload = json.dumps(
            {"query": query, "variables": {"org": org, "number": int(number)}}
        ).encode()
        req = urllib.request.Request(
            "https://api.github.com/graphql",
            data=payload,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "User-Agent": "solacecore-wiki-status",
            },
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode())

        proj = (data.get("data") or {}).get("organization", {}).get("projectV2")
        if not proj:
            return "> **Live project board:** query returned no project (check org/number/token scope).\n"

        nodes = proj["items"]["nodes"]
        # Tally by single-select "Status" field where present.
        buckets: dict[str, int] = {}
        for n in nodes:
            status = "Unassigned"
            for fv in (n.get("fieldValues") or {}).get("nodes", []):
                fname = (fv.get("field") or {}).get("name", "")
                if fname.lower() == "status" and fv.get("name"):
                    status = fv["name"]
                    break
            buckets[status] = buckets.get(status, 0) + 1

        lines = [f"**[{proj['title']}]({proj['url']})** — {len(nodes)} items\n", "| Status | Items |", "|--------|------:|"]
        for k, v in sorted(buckets.items(), key=lambda kv: (-kv[1], kv[0])):
            lines.append(f"| {k} | {v} |")
        return "\n".join(lines) + "\n"
    except Exception as exc:  # noqa: BLE001 - best-effort overlay
        return f"> **Live project board:** overlay failed ({exc.__class__.__name__}): {exc}. Showing in-repo status only.\n"


def build_page(repo_root: Path) -> str:
    now = _dt.datetime.now(_dt.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    repo = os.environ.get("GITHUB_REPOSITORY", "SolaceHarmony/SolaceCore")
    return (
        "<!-- topic: Orientation -->\n"
        "<!-- AUTO-GENERATED by tools/wiki/generate_project_status.py. Do not edit by hand. -->\n\n"
        "# Project Status\n\n"
        f"_Generated {now} for `{repo}`. Aggregates `docs/status/` plus an optional live project board._\n\n"
        "## Live Project Board\n\n"
        f"{project_board_section()}\n"
        "## In-Repo Status\n\n"
        f"{aggregate_status(repo_root)}\n"
    )


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="Generate wiki/Project-Status.md")
    ap.add_argument("--repo-root", default=".", help="Repository root")
    args = ap.parse_args(argv)

    repo_root = Path(args.repo_root).resolve()
    wiki_dir = repo_root / "wiki"
    wiki_dir.mkdir(exist_ok=True)
    (wiki_dir / "Project-Status.md").write_text(build_page(repo_root), encoding="utf-8")
    print(f"Wrote {wiki_dir / 'Project-Status.md'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

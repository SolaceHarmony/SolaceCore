#!/usr/bin/env python3
"""Generate wiki/Home.md from the SolaceCore wiki folder structure."""

from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path

from wiki_index import SECTION_ORDER, SUBTOPIC_ORDER, WikiPage, collect_pages, ordered


PREAMBLE = """\
# SolaceCore Wiki

Architecture notes, implementation plans, component design records, and workflow diagrams for SolaceCore. This wiki is managed from the repository via `.github/workflows/publish-wiki.yml`.

---
"""


def grouped_pages(pages: list[WikiPage]) -> dict[str, dict[str | None, list[WikiPage]]]:
    groups: dict[str, dict[str | None, list[WikiPage]]] = defaultdict(lambda: defaultdict(list))
    for page in pages:
        groups[page.topic][page.subtopic].append(page)
    return groups


def build_home(wiki_dir: Path) -> str:
    groups = grouped_pages(collect_pages(wiki_dir))
    lines = [PREAMBLE]

    for topic in ordered(set(groups), SECTION_ORDER):
        subtopics = groups[topic]
        lines.append("<details open>")
        lines.append(f"<summary><strong>{topic}</strong></summary>")
        lines.append("")

        for page in sorted(subtopics.get(None, []), key=lambda item: item.title.lower()):
            lines.append(f"- [{page.title}]({page.link})")

        if subtopics.get(None) and len(subtopics) > 1:
            lines.append("")

        subtopic_names = {name for name in subtopics if name is not None}
        for subtopic in ordered(subtopic_names, SUBTOPIC_ORDER.get(topic, [])):
            lines.append(f"**{subtopic}**")
            lines.append("")
            for page in sorted(subtopics[subtopic], key=lambda item: item.title.lower()):
                lines.append(f"- [{page.title}]({page.link})")
            lines.append("")

        lines.append("</details>")
        lines.append("")

    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate SolaceCore wiki Home page")
    parser.add_argument("--repo-root", required=True, help="Absolute path to repository root")
    args = parser.parse_args()

    wiki_dir = Path(args.repo_root).resolve() / "wiki"
    wiki_dir.mkdir(parents=True, exist_ok=True)
    content = build_home(wiki_dir)
    (wiki_dir / "Home.md").write_text(content, encoding="utf-8")
    print(f"Generated Home.md with {content.count('<details')} collapsible groups")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

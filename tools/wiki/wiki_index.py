#!/usr/bin/env python3
"""Shared wiki page discovery for SolaceCore wiki generators."""

from __future__ import annotations

from dataclasses import dataclass
import re
from pathlib import Path


GENERATED_PAGES = {"Home.md", "_Sidebar.md", "_footer.md", "Project-Status.md"}

SECTION_LABELS = {
    "architecture": "Architecture",
    "components": "Components",
    "planning": "Planning",
}

COMPONENT_LABELS = {
    "memory": "Memory",
    "middleware": "Middleware",
    "speech": "Speech",
    "vector-storage": "Vector Storage",
    "workflow": "Workflow",
}

SECTION_ORDER = ["Architecture", "Components", "Planning"]
SUBTOPIC_ORDER = {
    "Components": ["Workflow", "Middleware", "Memory", "Vector Storage", "Speech"],
}

TITLE_RE = re.compile(r"^#\s+(.+)", re.MULTILINE)
BOLD_TITLE_RE = re.compile(r"^\*\*(?:Title:\s*)?(.+?)\*\*", re.MULTILINE)


@dataclass(frozen=True)
class WikiPage:
    source: Path
    link: str
    title: str
    topic: str
    subtopic: str | None


def title_from_markdown(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    title_match = TITLE_RE.search(text)
    if title_match:
        return title_match.group(1).strip()

    bold_match = BOLD_TITLE_RE.search(text)
    if bold_match:
        return bold_match.group(1).strip()

    return path.stem.replace("-", " ").replace("_", " ").title()


def topic_for(relative_path: Path) -> tuple[str, str | None]:
    parts = relative_path.parts
    if not parts:
        return "Other", None

    root = parts[0]
    topic = SECTION_LABELS.get(root, root.replace("-", " ").title())

    if root == "components" and len(parts) > 2:
        component = parts[1]
        return topic, COMPONENT_LABELS.get(component, component.replace("-", " ").title())

    return topic, None


def wiki_link_for(relative_path: Path) -> str:
    return relative_path.with_suffix("").as_posix()


def collect_pages(wiki_dir: Path) -> list[WikiPage]:
    pages: list[WikiPage] = []
    for page in sorted(wiki_dir.rglob("*.md")):
        relative_path = page.relative_to(wiki_dir)
        if page.name in GENERATED_PAGES or any(part.startswith(".") for part in relative_path.parts):
            continue

        topic, subtopic = topic_for(relative_path)
        pages.append(
            WikiPage(
                source=relative_path,
                link=wiki_link_for(relative_path),
                title=title_from_markdown(page),
                topic=topic,
                subtopic=subtopic,
            )
        )
    return pages


def ordered(values: set[str], preferred: list[str]) -> list[str]:
    selected = [value for value in preferred if value in values]
    selected.extend(sorted(value for value in values if value not in preferred))
    return selected

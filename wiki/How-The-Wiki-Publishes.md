<!-- topic: Reference -->

# How the Wiki Publishes

The wiki is source-controlled in this repository under `wiki/` and published to the GitHub Wiki by `.github/workflows/publish-wiki.yml`.

## Publish Triggers

The workflow runs when one of these paths changes on `main`, `development`, or `master`:

- `wiki/**`
- `tools/wiki/**`
- `.github/workflows/publish-wiki.yml`
- `docs/status/**`

It also runs daily at `06:17 UTC` so generated status pages can refresh even when no source file changed.

## Generation Steps

1. `tools/wiki/sync_pages.py` mirrors selected authoritative docs into wiki pages.
2. `tools/wiki/generate_project_status.py` rebuilds `wiki/Project-Status.md` from `docs/status/` and optionally overlays live GitHub Project data.
3. `tools/wiki/generate_sidebar.py` rebuilds `wiki/_Sidebar.md` from each page's `<!-- topic: ... -->` marker.
4. The Action syncs the resulting `wiki/` directory to the repository GitHub Wiki remote.

## Edit Rules

Hand-authored pages live directly in `wiki/`.

Mirrored pages are generated from their source documents. When a page says it is mirrored, edit the source under `docs/`, not the generated wiki page.

Generated files include a marker at the top. Do not edit those by hand unless you are also changing the generator.

## Optional Project Board Overlay

`Project-Status.md` can include GitHub Projects data when these values are configured:

- `PROJECTS_READ_TOKEN`: repository secret with `read:project`
- `PROJECT_ORG`: repository variable
- `PROJECT_NUMBER`: repository variable

When those values are absent, the generator still succeeds and publishes the in-repo status documents only.

---
This keeps the wiki readable as a GitHub Wiki while preserving the repository as the source of truth.

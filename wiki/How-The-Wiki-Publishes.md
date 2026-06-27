<!-- topic: Reference -->

# How the Wiki Publishes

The wiki is source-controlled in this repository under `wiki/` and published to the GitHub Wiki by `.github/workflows/publish-wiki.yml`.

## Publish Triggers

The workflow runs when one of these paths changes on `main`, `development`, or `master`:

- `wiki/**`
- `tools/wiki/**`
- `.github/workflows/publish-wiki.yml`

It also runs daily at `06:17 UTC` so generated status pages can refresh even when no source file changed.

## Generation Steps

1. `tools/wiki/generate_project_status.py` rebuilds `wiki/Project-Status.md` from the status pages in `wiki/` and optionally overlays live GitHub Project data.
2. `tools/wiki/generate_sidebar.py` rebuilds `wiki/_Sidebar.md` from each page's `<!-- topic: ... -->` marker.
3. The Action syncs the resulting `wiki/` directory to the repository GitHub Wiki remote.

## Edit Rules

Hand-authored pages live directly in `wiki/`.

Generated files include a marker at the top. Do not edit those by hand unless you are also changing the generator.

## Optional Project Board Overlay

`Project-Status.md` can include GitHub Projects data when these values are configured:

- `PROJECTS_READ_TOKEN`: repository secret with `read:project`
- `PROJECT_ORG`: repository variable
- `PROJECT_NUMBER`: repository variable

When those values are absent, the generator still succeeds and publishes the in-repo status documents only.

---
This keeps the wiki readable as a GitHub Wiki while preserving the repository as the source of truth.

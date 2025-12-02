#!/usr/bin/env bash
# Pure bash/awk watcher (no expect). Fires a screenshot whenever the
# regex matches a log line. Continues until the app exits. Uses line
# buffering via stdbuf if available; otherwise still works but may buffer.
# Usage: scripts/watch_and_shoot.sh 'Started actor:|Paused actor:|Stopped actor:'

set -euo pipefail
pattern=${1:-}
if [[ -z "$pattern" ]]; then
  echo "Usage: $0 '{regex-pattern}'" >&2
  exit 1
fi

if command -v stdbuf >/dev/null 2>&1; then
  stdbuf -oL ./gradlew :composeApp:run |
  awk -v pat="$pattern" '
    $0 ~ pat { system("python tools/gpt5_screenshot.py tail 1 1"); print; next }
    { print }
  '
else
  # Fallback without stdbuf; output may be block-buffered
  ./gradlew :composeApp:run |
  awk -v pat="$pattern" '
    $0 ~ pat { system("python tools/gpt5_screenshot.py tail 1 1"); print; next }
    { print }
  '
fi


#!/bin/bash
# Script to create GitHub issues from docs/status/roadmap_issues.json
# Usage: ./scripts/create_roadmap_issues.sh

set -e

REPO="SolaceHarmony/SolaceCore"
ISSUES_FILE="docs/status/roadmap_issues.json"

if [ ! -f "$ISSUES_FILE" ]; then
    echo "Error: $ISSUES_FILE not found"
    exit 1
fi

# Check if gh CLI is installed and authenticated
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed"
    echo "Install from: https://cli.github.com/"
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo "Error: GitHub CLI is not authenticated"
    echo "Run: gh auth login"
    exit 1
fi

echo "Creating GitHub issues from $ISSUES_FILE..."
echo "Repository: $REPO"
echo ""

# Parse JSON and create issues
issues_count=$(jq length "$ISSUES_FILE")
created=0
failed=0

for i in $(seq 0 $((issues_count - 1))); do
    title=$(jq -r ".[$i].title" "$ISSUES_FILE")
    body=$(jq -r ".[$i].body" "$ISSUES_FILE")
    labels=$(jq -r ".[$i].labels | join(\",\")" "$ISSUES_FILE")
    
    echo "Creating issue: $title"
    
    if gh issue create \
        --repo "$REPO" \
        --title "$title" \
        --body "$body" \
        --label "$labels" > /dev/null 2>&1; then
        echo "  ✓ Created"
        ((created++))
    else
        echo "  ✗ Failed"
        ((failed++))
    fi
done

echo ""
echo "Summary:"
echo "  Created: $created"
echo "  Failed: $failed"
echo "  Total: $issues_count"

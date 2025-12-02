#!/usr/bin/env python3
"""
Create GitHub issues from docs/status/roadmap_issues.json using GitHub API.
Requires: pip install PyGithub
Usage: GITHUB_TOKEN=<your-token> python3 scripts/create_issues_api.py
"""

import json
import os
import sys

try:
    from github import Github
except ImportError:
    print("Error: PyGithub not installed")
    print("Install with: pip install PyGithub")
    sys.exit(1)

def create_issues():
    # Get GitHub token from environment
    token = os.environ.get('GITHUB_TOKEN')
    if not token:
        print("Error: GITHUB_TOKEN environment variable not set")
        print("Usage: GITHUB_TOKEN=<your-token> python3 scripts/create_issues_api.py")
        sys.exit(1)
    
    # Load issues from JSON
    with open('docs/status/roadmap_issues.json', 'r') as f:
        issues_data = json.load(f)
    
    # Connect to GitHub
    print(f"Connecting to GitHub...")
    g = Github(token)
    
    try:
        repo = g.get_repo("SolaceHarmony/SolaceCore")
        print(f"Repository: {repo.full_name}")
        print(f"Creating {len(issues_data)} issues...\n")
    except Exception as e:
        print(f"Error accessing repository: {e}")
        sys.exit(1)
    
    # Create issues
    created = 0
    failed = 0
    
    for issue_data in issues_data:
        title = issue_data['title']
        body = issue_data['body']
        labels = issue_data['labels']
        
        print(f"Creating: {title}")
        
        try:
            issue = repo.create_issue(
                title=title,
                body=body,
                labels=labels
            )
            print(f"  ✓ Created: {issue.html_url}")
            created += 1
        except Exception as e:
            print(f"  ✗ Failed: {e}")
            failed += 1
    
    print(f"\nSummary:")
    print(f"  Created: {created}")
    print(f"  Failed: {failed}")
    print(f"  Total: {len(issues_data)}")

if __name__ == '__main__':
    create_issues()

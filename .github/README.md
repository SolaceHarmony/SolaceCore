# GitHub Workflows

## CodeQL Security Scanning

This repository uses CodeQL for automated security vulnerability scanning.

### Configuration

The CodeQL workflow (`.github/workflows/codeql.yml`) is configured to:
- Scan **Java/Kotlin** code (the primary language used in this Kotlin Multiplatform project)
- Run on pushes to `main` branch
- Run on pull requests to `main` branch  
- Run weekly on Sundays (scheduled scan)

### Why This Configuration?

Previously, the GitHub default setup was attempting to scan for "GitHub Actions" code, which resulted in failures because:
1. This repository is primarily a Kotlin Multiplatform project
2. There is no JavaScript/TypeScript Actions code to scan
3. The CodeQL scanner couldn't find any relevant code

By creating this custom workflow, we explicitly configure CodeQL to scan the correct language (Java/Kotlin) for security vulnerabilities.

### Maintenance

If you need to modify the CodeQL configuration:
- Edit `.github/workflows/codeql.yml`
- Change the `language` matrix if you add new languages to the project
- Adjust the schedule if you want different scan frequencies
- Add custom queries if needed for specific security checks

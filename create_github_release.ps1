#!/usr/bin/env pwsh
# create_github_release.ps1 — Push v1.0.0 tag and create GitHub release with APK
# Usage: .\create_github_release.ps1

$ErrorActionPreference = "Stop"
$version = "1.0.0"
$tag = "v$version"
$repoName = "word-journey"
$apkPath = "releases\v1.0.0\word-journeys-v1.0.0-release.apk"
$notesPath = "releases\v1.0.0\release_notes.md"

Write-Host "=== Word Journeys Release $tag ===" -ForegroundColor Cyan

# Verify APK exists
if (-not (Test-Path $apkPath)) {
    Write-Host "ERROR: APK not found at $apkPath" -ForegroundColor Red
    Write-Host "Run '.\gradlew assembleRelease' first."
    exit 1
}

# Verify gh auth
Write-Host "`nChecking GitHub CLI authentication..."
gh auth status
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Not authenticated. Run 'gh auth login' first." -ForegroundColor Red
    exit 1
}

# Create repo if it doesn't exist
Write-Host "`nEnsuring GitHub repo exists..."
$repoCheck = gh repo view "djtaylor333/$repoName" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating repo djtaylor333/$repoName..."
    gh repo create $repoName --public --description "Word Journeys - A level-based word puzzle game for Android" --source . --remote origin --push
} else {
    Write-Host "Repo already exists."
    # Ensure remote is set
    $remotes = git remote
    if ($remotes -notcontains "origin") {
        git remote add origin "git@github.com:djtaylor333/$repoName.git"
    }
}

# Commit any uncommitted changes
Write-Host "`nCommitting changes..."
git add -A
$status = git status --porcelain
if ($status) {
    git commit -m "Release $tag - Word Journeys initial release"
}

# Push to main
Write-Host "`nPushing to main..."
$branch = git branch --show-current
git push -u origin $branch

# Create and push tag
Write-Host "`nCreating tag $tag..."
$existingTag = git tag -l $tag
if ($existingTag) {
    Write-Host "Tag $tag already exists, skipping tag creation."
} else {
    git tag -a $tag -m "Word Journeys $tag - Initial Release"
}
git push origin $tag

# Create GitHub Release
Write-Host "`nCreating GitHub Release..."
gh release create $tag $apkPath `
    --title "Word Journeys $tag" `
    --notes-file $notesPath `
    --latest

Write-Host "`n=== Release $tag created successfully! ===" -ForegroundColor Green
Write-Host "View at: https://github.com/djtaylor333/$repoName/releases/tag/$tag"

<#
.SYNOPSIS
  Bumps version, commits, tags, and pushes to trigger a GitHub Release.

.PARAMETER BumpType
  Which part of the version to bump: major, minor, or patch (default: patch)

.EXAMPLE
  .\build-release.ps1 -BumpType minor
#>
param(
    [ValidateSet("major","minor","patch")]
    [string]$BumpType = "patch"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# ── Read current version ──────────────────────────────────────────────────────
$versionFile = "version.json"
$versionJson = Get-Content $versionFile | ConvertFrom-Json
$parts = $versionJson.versionName.Split(".")
$major = [int]$parts[0]
$minor = [int]$parts[1]
$patch = [int]$parts[2]

switch ($BumpType) {
    "major" { $major++; $minor = 0; $patch = 0 }
    "minor" { $minor++; $patch = 0 }
    "patch" { $patch++ }
}

$newVersion = "$major.$minor.$patch"
$newCode = $versionJson.versionCode + 1

Write-Host "Bumping version: $($versionJson.versionName) -> $newVersion (code $newCode)" -ForegroundColor Cyan

# ── Update version.json ──────────────────────────────────────────────────────
$versionJson.versionName = $newVersion
$versionJson.versionCode = $newCode
$versionJson | ConvertTo-Json -Depth 2 | Set-Content $versionFile

# ── Update app/build.gradle ──────────────────────────────────────────────────
$buildGradle = Get-Content "app/build.gradle" -Raw
$buildGradle = $buildGradle -replace 'versionCode \d+', "versionCode $newCode"
$buildGradle = $buildGradle -replace 'versionName "[\d.]+"', "versionName `"$newVersion`""
Set-Content "app/build.gradle" $buildGradle

# ── Git operations ───────────────────────────────────────────────────────────
$tag = "v$newVersion"

git add -A
git commit -m "Release $tag"
git tag $tag
git push origin main
git push origin $tag

Write-Host ""
Write-Host "Released $tag successfully!" -ForegroundColor Green
Write-Host "GitHub Actions will build the APK and create the release automatically." -ForegroundColor Yellow

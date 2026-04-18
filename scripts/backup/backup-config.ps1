. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

$timestamp = (Get-Date).ToString("yyyyMMdd-HHmmss")
$configRoot = Join-Path $BackupRoot "config"
Ensure-Dir $configRoot

$tempDir = Join-Path $configRoot "_tmp"
Ensure-Dir $tempDir

Write-Log "Starting config backup..."

foreach ($path in $ConfigPaths) {
  if (Test-Path $path) {
    $name = Split-Path $path -Leaf
    Copy-Item -Path $path -Destination (Join-Path $tempDir $name) -Force
  }
}

$zipFile = Join-Path $configRoot ("config-$timestamp.zip")
Zip-Path $tempDir $zipFile
Remove-Item $tempDir -Recurse -Force

Prune-Old $configRoot "config-*.zip" $RetentionConfig
Write-Log "Config backup saved: $zipFile"

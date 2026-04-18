. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

$timestamp = (Get-Date).ToString("yyyyMMdd-HHmmss")
$filesRoot = Join-Path $BackupRoot "files"
Ensure-Dir $filesRoot

$zipFile = Join-Path $filesRoot ("files-$timestamp.zip")
Write-Log "Starting files backup..."

if (-not (Test-Path $UploadsPath)) {
  Write-Log "Uploads path not found: $UploadsPath"
  exit 1
}

Zip-Path $UploadsPath $zipFile
Prune-Old $filesRoot "files-*.zip" $RetentionFiles
Write-Log "Files backup saved: $zipFile"

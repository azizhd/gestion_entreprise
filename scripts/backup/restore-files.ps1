param(
  [string]$BackupZip
)

. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

if (-not $BackupZip -or -not (Test-Path $BackupZip)) {
  Write-Log "Backup zip not found."
  exit 1
}

if (-not (Test-Path $UploadsPath)) { Ensure-Dir $UploadsPath }
Expand-Archive -Path $BackupZip -DestinationPath $UploadsPath -Force
Write-Log "Files restore completed."

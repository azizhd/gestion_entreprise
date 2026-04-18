param(
  [string]$BackupZip
)

. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

if (-not $BackupZip -or -not (Test-Path $BackupZip)) {
  Write-Log "Backup zip not found."
  exit 1
}

$tempDir = Join-Path $BackupRoot "_restore_config"
Ensure-Dir $tempDir
Expand-Archive -Path $BackupZip -DestinationPath $tempDir -Force

foreach ($path in $ConfigPaths) {
  $name = Split-Path $path -Leaf
  $restoreFile = Join-Path $tempDir $name
  Copy-If-Exists $restoreFile $path
}

Remove-Item $tempDir -Recurse -Force
Write-Log "Config restore completed."

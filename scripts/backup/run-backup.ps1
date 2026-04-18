param(
  [switch]$FullDb,
  [switch]$IncrementalDb,
  [switch]$Files,
  [switch]$Config
)

. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

Write-Log "Backup started."

if ($FullDb) { & $PSScriptRoot\\backup-db.ps1 -Mode Full }
if ($IncrementalDb) { & $PSScriptRoot\\backup-db.ps1 -Mode Incremental }
if ($Files) { & $PSScriptRoot\\backup-files.ps1 }
if ($Config) { & $PSScriptRoot\\backup-config.ps1 }

if ($ExternalCopyPath) {
  Write-Log "Syncing to external path: $ExternalCopyPath"
  Ensure-Dir $ExternalCopyPath
  Copy-Item -Path $BackupRoot\* -Destination $ExternalCopyPath -Recurse -Force
}

Write-Log "Backup completed."

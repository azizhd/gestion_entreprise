param(
  [string]$BackupZip
)

. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

if (-not $BackupZip -or -not (Test-Path $BackupZip)) {
  Write-Log "Backup zip not found."
  exit 1
}

$tempDir = Join-Path $BackupRoot "_restore_db"
Ensure-Dir $tempDir
Expand-Archive -Path $BackupZip -DestinationPath $tempDir -Force

$sqlFile = Get-ChildItem -Path $tempDir -Filter "*.sql" | Select-Object -First 1
if (-not $sqlFile) {
  Write-Log "No SQL file in archive."
  exit 1
}

$env:MYSQL_PWD = $MysqlPassword
& $MysqlPath --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser $MysqlDatabase < $sqlFile.FullName

Remove-Item $tempDir -Recurse -Force
Write-Log "Database restore completed."

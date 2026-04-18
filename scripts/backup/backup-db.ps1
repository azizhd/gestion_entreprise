param(
  [ValidateSet("Full","Incremental")]
  [string]$Mode = "Full"
)

. "$PSScriptRoot\\backup.config.ps1"
. "$PSScriptRoot\\backup-helpers.ps1"

$timestamp = (Get-Date).ToString("yyyyMMdd-HHmmss")
$dbRoot = Join-Path $BackupRoot "db"
$fullDir = Join-Path $dbRoot "full"
$incDir = Join-Path $dbRoot "incremental"
$stateFile = Join-Path $dbRoot "binlog-state.json"

Ensure-Dir $fullDir
Ensure-Dir $incDir

$env:MYSQL_PWD = $MysqlPassword

if ($Mode -eq "Full") {
  $dumpFile = Join-Path $fullDir ("db-full-$timestamp.sql")
  $zipFile = Join-Path $fullDir ("db-full-$timestamp.zip")
  Write-Log "Starting full DB backup..."
  & $MysqldumpPath --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --databases $MysqlDatabase --single-transaction --routines --events --triggers --flush-logs --master-data=2 > $dumpFile
  Zip-Path $dumpFile $zipFile
  Remove-Item $dumpFile -Force
  Prune-Old $fullDir "db-full-*.zip" $RetentionDbFull
  Write-Log "Full DB backup saved: $zipFile"
  exit 0
}

# Incremental backup using binlog (requires MySQL binlog enabled)
Write-Log "Starting incremental DB backup..."
$incFile = Join-Path $incDir ("db-inc-$timestamp.sql")
$zipFile = Join-Path $incDir ("db-inc-$timestamp.zip")

try {
  if (-not (Test-Path $stateFile)) {
    # Initialize state from current master status
    $status = & $MysqlPath --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser -e "SHOW MASTER STATUS\\G"
    $binlog = ($status | Select-String "File:").ToString().Split(":")[1].Trim()
    $pos = [int](($status | Select-String "Position:").ToString().Split(":")[1].Trim())
    $state = @{ file = $binlog; position = $pos } | ConvertTo-Json
    $state | Out-File -FilePath $stateFile -Encoding utf8
    Write-Log "Binlog state initialized. Next run will collect changes."
    exit 0
  }

  $state = Get-Content $stateFile | ConvertFrom-Json
  $binlogFile = $state.file
  $startPos = $state.position

  & $MysqlBinlogPath --read-from-remote-server --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --start-position=$startPos $binlogFile > $incFile

  # Update state to current master position
  $status = & $MysqlPath --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser -e "SHOW MASTER STATUS\\G"
  $newBinlog = ($status | Select-String "File:").ToString().Split(":")[1].Trim()
  $newPos = [int](($status | Select-String "Position:").ToString().Split(":")[1].Trim())
  $newState = @{ file = $newBinlog; position = $newPos } | ConvertTo-Json
  $newState | Out-File -FilePath $stateFile -Encoding utf8

  Zip-Path $incFile $zipFile
  Remove-Item $incFile -Force
  Prune-Old $incDir "db-inc-*.zip" $RetentionDbIncremental
  Write-Log "Incremental DB backup saved: $zipFile"
}
catch {
  Write-Log "Incremental backup failed. Falling back to full backup."
  & $PSScriptRoot\\backup-db.ps1 -Mode Full
}

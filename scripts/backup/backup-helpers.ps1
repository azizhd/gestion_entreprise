function Ensure-Dir([string]$Path) {
  if (-not (Test-Path $Path)) { New-Item -ItemType Directory -Path $Path | Out-Null }
}

function Write-Log([string]$Message) {
  $ts = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
  Write-Host "[$ts] $Message"
}

function Prune-Old([string]$Path, [string]$Filter, [int]$Keep) {
  if (-not (Test-Path $Path)) { return }
  $items = Get-ChildItem -Path $Path -Filter $Filter | Sort-Object LastWriteTime -Descending
  if ($items.Count -le $Keep) { return }
  $items | Select-Object -Skip $Keep | ForEach-Object { Remove-Item -Force $_.FullName }
}

function Zip-Path([string]$Source, [string]$DestinationZip) {
  if (Test-Path $DestinationZip) { Remove-Item $DestinationZip -Force }
  Compress-Archive -Path $Source -DestinationPath $DestinationZip
}

function Copy-If-Exists([string]$Source, [string]$Destination) {
  if (Test-Path $Source) {
    Copy-Item -Path $Source -Destination $Destination -Force
  }
}

function Send-BackupNotification([string]$Subject, [string]$Body) {
  if (-not $script:NotifyEmail -or -not $script:NotifyFrom -or -not $script:SmtpServer) {
    return
  }
  Send-MailMessage -To $script:NotifyEmail -From $script:NotifyFrom -Subject $Subject -Body $Body -SmtpServer $script:SmtpServer -Port $script:SmtpPort
}

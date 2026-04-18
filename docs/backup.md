# Local Backup System (Dev)

## Overview
This project ships simple PowerShell scripts to back up:
- MySQL database
- Uploaded files (documents)
- Spring Boot config files

Scripts live in [scripts/backup](../scripts/backup).

## Folder Structure
Default location (configurable):

```
C:\backups\saas-dev\
  db\
    full\
    incremental\
    binlog-state.json
  files\
  config\
```

## Configuration
Edit [scripts/backup/backup.config.ps1](../scripts/backup/backup.config.ps1):
- MySQL credentials
- Backup root
- Uploads path
- Retention counts

## Schedules (Recommended)
Use Windows Task Scheduler.

- **DB incremental (every 2 hours)**
  - Program: `powershell.exe`
  - Arguments: `-ExecutionPolicy Bypass -File scripts\backup\run-backup.ps1 -IncrementalDb`

- **DB full (daily)**
  - Arguments: `-ExecutionPolicy Bypass -File scripts\backup\run-backup.ps1 -FullDb`

- **Files full (daily)**
  - Arguments: `-ExecutionPolicy Bypass -File scripts\backup\run-backup.ps1 -Files`

- **Config (weekly)**
  - Arguments: `-ExecutionPolicy Bypass -File scripts\backup\run-backup.ps1 -Config`

## Restore
- **Database**
  - `powershell -ExecutionPolicy Bypass -File scripts\backup\restore-db.ps1 -BackupZip C:\backups\saas-dev\db\full\db-full-YYYYMMDD-HHMMSS.zip`
- **Files**
  - `powershell -ExecutionPolicy Bypass -File scripts\backup\restore-files.ps1 -BackupZip C:\backups\saas-dev\files\files-YYYYMMDD-HHMMSS.zip`
- **Config**
  - `powershell -ExecutionPolicy Bypass -File scripts\backup\restore-config.ps1 -BackupZip C:\backups\saas-dev\config\config-YYYYMMDD-HHMMSS.zip`

## Notes
- Incremental DB backups use MySQL binlog (`mysqlbinlog`). Ensure binlog is enabled.
- Backups are timestamped for easy restore.
- Do not commit backup folders to Git.
- External copy can be enabled with `ExternalCopyPath`.

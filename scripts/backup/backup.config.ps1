# Backup configuration for dev-stage (PowerShell)
# Adjust values to match your local setup.

$BackupRoot = "C:\\backups\\saas-dev"
$ExternalCopyPath = $null  # Example: "E:\\backups\\saas-dev"

# MySQL connection
$MysqlHost = "localhost"
$MysqlPort = 3306
$MysqlUser = "root"
$MysqlPassword = ""
$MysqlDatabase = "gestion_entreprise"

# Binaries (optional if in PATH)
$MysqldumpPath = "mysqldump"
$MysqlPath = "mysql"
$MysqlBinlogPath = "mysqlbinlog"

# Storage folders
$UploadsPath = "C:\\Users\\aziz polytech\\Desktop\\Gestion_entreprise\\backend\\uploads\\documents"

# Config files to backup
$ConfigPaths = @(
  "C:\\Users\\aziz polytech\\Desktop\\Gestion_entreprise\\backend\\src\\main\\resources\\application.properties"
)

# Retention
$RetentionDbFull = 7
$RetentionDbIncremental = 84  # 7 days x 12 per day (2-hourly)
$RetentionFiles = 7
$RetentionConfig = 4

# Optional notification
$NotifyEmail = $null
$NotifyFrom = $null
$SmtpServer = $null
$SmtpPort = 587

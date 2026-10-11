# =====================================================================
# init-uat-db.ps1 —— 初始化 UAT 数据库（ry-cloud-uat）
#
# 用法（仓库根目录执行）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\init-uat-db.ps1
#
# 做三件事：
#   1. 删掉并重建 ry-cloud-uat 库（幂等：每次执行都是干净的种子）
#   2. 从 DEV 的 ry-cloud 整库导出（表结构 + 数据）灌进去——
#      菜单、用户、博客示例数据全部继承，当 UAT 测试种子
#   3. 校验表数量与 DEV 一致
#
# 注意：dump/导入全在 mysql-server 容器内完成（不走宿主机管道），
#       避免 PowerShell 管道把 UTF-8 中文转成 GBK 导致乱码。
# =====================================================================
$ErrorActionPreference = 'Stop'

$mysql = 'mysql -uroot -p123456'
$dev = 'ry-cloud'
$uat = 'ry-cloud-uat'

Write-Host "==> 重建 $uat 库..."
docker exec mysql-server sh -c "$mysql -e 'DROP DATABASE IF EXISTS ``$uat``; CREATE DATABASE ``$uat`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;'" | Out-Null

Write-Host "==> 从 $dev 导出种子（容器内进行，避免编码损耗）..."
docker exec mysql-server sh -c "mysqldump -uroot -p123456 --no-tablespaces $dev > /tmp/uat-seed.sql"
if ($LASTEXITCODE -ne 0) { throw "mysqldump 失败" }

Write-Host "==> 灌入 $uat ..."
docker exec mysql-server sh -c "$mysql --default-character-set=utf8mb4 $uat < /tmp/uat-seed.sql"
if ($LASTEXITCODE -ne 0) { throw "导入失败" }

docker exec mysql-server sh -c "rm -f /tmp/uat-seed.sql"

# 校验：SQL 写进临时文件再 docker cp 进容器执行（引号零损耗）
$countSql = Join-Path $env:TEMP 'uat-count.sql'
Set-Content -Path $countSql -Encoding ASCII -Value "SELECT 'dev', COUNT(*) FROM information_schema.tables WHERE table_schema='ry-cloud'; SELECT 'uat', COUNT(*) FROM information_schema.tables WHERE table_schema='ry-cloud-uat';"
docker cp $countSql mysql-server:/tmp/uat-count.sql | Out-Null
$counts = docker exec mysql-server sh -c "mysql -uroot -p123456 -N < /tmp/uat-count.sql"
Remove-Item $countSql -ErrorAction SilentlyContinue
$devTables = ($counts | Where-Object { $_ -like 'dev*' }) -replace 'dev\s*',''
$uatTables = ($counts | Where-Object { $_ -like 'uat*' }) -replace 'uat\s*',''
Write-Host ""
Write-Host "完成：$uat 共 $uatTables 张表（DEV $dev 为 $devTables 张）"
if ($uatTables -ne $devTables) { Write-Warning "表数量不一致，请检查导入日志" }

Write-Host ""
Write-Host "后续步骤："
Write-Host "  1. powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-up.ps1"
Write-Host "  2. cd web; yarn dev:uat"

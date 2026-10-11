# =====================================================================
# uat-down.ps1 —— 停止 UAT 环境的全部后端服务
#
# 用法（仓库根目录执行）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-down.ps1
#
# 只杀 UAT 端口（8001/8011/8020/8890/81），不碰 DEV（80/8000/8010/8889）。
# 前端 UAT（81）是 vite node 进程，一并杀掉。
# =====================================================================
$ErrorActionPreference = 'Continue'

$uatPorts = 8001, 8011, 8020, 8890, 81

foreach ($port in $uatPorts) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        Write-Host "  停 $port <- PID $($conn.OwningProcess) $($proc.ProcessName)"
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    } else {
        Write-Host "  $port 空闲（未在运行）"
    }
}

Start-Sleep -Seconds 3
Write-Host ""
Write-Host "== 复核 =="
$anyAlive = $false
foreach ($port in $uatPorts) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "  $port 仍被占用" -ForegroundColor Red
        $anyAlive = $true
    }
}
if (-not $anyAlive) { Write-Host "  UAT 全部端口已释放" -ForegroundColor Green }

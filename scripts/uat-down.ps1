# =====================================================================
# uat-down.ps1 —— 停止 UAT 环境的全部服务
#
# 用法（仓库根目录执行）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-down.ps1
#
# 只处理 UAT 端口（8001/8011/8020/8890/81），不碰 DEV（80/8000/8010/8889）。
#
# 安全策略：先判定进程来源，**IDE（IDEA 带 debug agent）启动的不杀**，
# 只提示你到 IDE 里停止——避免把调试会话打断（与 dev 环境的既有约定一致）。
# =====================================================================
# 用 Continue 而非 Stop：某个端口查不到、进程已先退出时，
# 不能因此中断其余端口的回收流程。
$ErrorActionPreference = 'Continue'

# UAT 独占端口：三个后端 + 网关 + 前端 vite（yarn dev:uat 固定 81）。
# 81 也纳入回收，否则重启后旧前端进程会一直占着端口。
$uatPorts = 8001, 8011, 8020, 8890, 81

# 依据命令行特征判断进程是否由 IDE 启动（IDEA 会带 debug agent、
# TieredStopAtLevel、JetBrains 等标志）；取不到命令行时按"非 IDE"处理。
function Test-IdeProcess {
    param([int]$ProcId)
    $ci = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcId" -ErrorAction SilentlyContinue
    if (-not $ci -or -not $ci.CommandLine) { return $false }
    $cmd = $ci.CommandLine
    return ($cmd -match 'TieredStopAtLevel=1' -or $cmd -match 'idea_rt\.jar' -or
            $cmd -match 'spring\.output\.ansi\.enabled=always' -or $cmd -match 'JetBrains' -or
            $cmd -match 'agentlib:jdwp')
}

# 记录按策略保留的端口（IDE 进程），复核时用来区分"正常保留"与"没杀干净"。
$skipped = @()
foreach ($port in $uatPorts) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $conn) { Write-Host "  $port 空闲（未在运行）"; continue }

    $procId = $conn.OwningProcess
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if (Test-IdeProcess $procId) {
        Write-Host ("  跳过 {0} <- PID {1} {2}（IDE 启动，请到 IDE 里停止）" -f $port, $procId, $proc.ProcessName) -ForegroundColor Yellow
        $skipped += $port
        continue
    }
    Write-Host "  停 $port <- PID $procId $($proc.ProcessName)"
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}

# 进程退出后端口释放有延迟，等 3 秒再复核，避免误报"仍被占用"。
Start-Sleep -Seconds 3
Write-Host ''
Write-Host '== 复核 =='
# 收集仍在监听的端口，正常应为空。
$alive = @()
foreach ($port in $uatPorts) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { $alive += $port }
}
if (-not $alive) {
    Write-Host '  UAT 全部端口已释放' -ForegroundColor Green
} else {
    foreach ($port in $alive) {
        if ($skipped -contains $port) { Write-Host "  $port 仍在监听（IDE 进程，按策略保留）" -ForegroundColor Yellow }
        else { Write-Host "  $port 仍被占用" -ForegroundColor Red }
    }
}
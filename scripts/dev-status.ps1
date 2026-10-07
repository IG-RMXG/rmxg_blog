# 本地开发服务状态巡检
#
# 用法（在仓库根目录执行；注意本机没有 pwsh，用 powershell）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\dev-status.ps1
#
# 输出：Docker 中间件健康状态、应用服务的端口/PID/进程名/启动来源（IDE 或终端）、
#       HTTP 探针耗时，以及登录链路快检。只读，不启动也不停止任何东西。

$ErrorActionPreference = 'SilentlyContinue'

function Get-PortOwner {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if (-not $conn) { return $null }
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue

    # 判定启动来源：IDE 启动 Spring Boot 时会带上这些标志
    # （-XX:TieredStopAtLevel=1 与 -Dspring.output.ansi.enabled=always）。
    # 注意不能只看进程路径：IDEA 是用项目 JDK 17 启动的，命令行里没有 idea/JetBrains 字样。
    $cmd = ''
    $ci = Get-CimInstance Win32_Process -Filter "ProcessId=$($conn.OwningProcess)" -ErrorAction SilentlyContinue
    if ($ci -and $ci.CommandLine) { $cmd = $ci.CommandLine }

    $isIde = $cmd -match 'TieredStopAtLevel=1' -or
             $cmd -match 'spring\.output\.ansi\.enabled=always' -or
             $cmd -match 'idea_rt\.jar' -or
             $cmd -match 'idea\.version=' -or
             $cmd -match 'JetBrains'

    # 说明：只有 Java 服务能可靠判定来源（靠 Spring Boot 的 IDE 启动标志）。
    # 前端 vite 由 WebStorm 经 shell 拉起 yarn 再拉起 node，中间父进程已退出，
    # 进程链回溯不到 IDE（实测父进程是 cmd.exe），所以不臆断为"终端"，统一记非 IDE。
    return [pscustomobject]@{
        PID    = $conn.OwningProcess
        Name   = if ($proc) { $proc.ProcessName } else { '?' }
        Path   = if ($proc) { $proc.Path } else { '' }
        Origin = if ($isIde) { 'IDE' } else { '非IDE' }
        Cmd    = $cmd
    }
}

Write-Host ''
Write-Host '==================== Docker 中间件 ====================' -ForegroundColor Cyan
$containers = docker ps -a --format '{{.Names}}|{{.Status}}|{{.Ports}}' 2>$null
if ($containers) {
    foreach ($line in $containers) {
        $parts  = $line -split '\|'
        $name   = $parts[0]
        $status = $parts[1]
        $color  = if ($status -match 'healthy') { 'Green' } elseif ($status -match '^Up') { 'Yellow' } else { 'Red' }
        Write-Host ("  {0,-16} {1}" -f $name, $status) -ForegroundColor $color
    }
} else {
    Write-Host '  未检测到容器（Docker 未运行？）' -ForegroundColor Red
}

Write-Host ''
Write-Host '==================== 应用服务 ========================' -ForegroundColor Cyan

$services = @(
    @{ Name = '前端 vite';        Port = 80;   Url = 'http://127.0.0.1:80/';                    Probe = 'HTTP' }
    @{ Name = 'system-service';   Port = 8000; Url = 'http://127.0.0.1:8000/system/';           Probe = 'HTTP' }
    @{ Name = 'gateway-service';  Port = 8889; Url = 'http://127.0.0.1:8889/';                  Probe = 'HTTP' }
    @{ Name = 'RuoYi 网关';       Port = 8080; Url = 'http://127.0.0.1:8080/';                  Probe = 'HTTP' }
    @{ Name = 'RuoYi 认证';       Port = 9200; Url = 'http://127.0.0.1:9200/';                  Probe = 'HTTP' }
    @{ Name = 'RuoYi 系统模块';   Port = 9201; Url = 'http://127.0.0.1:9201/';                  Probe = 'HTTP' }
)

foreach ($svc in $services) {
    $owner = Get-PortOwner -Port $svc.Port
    if (-not $owner) {
        Write-Host ("  {0,-18} :{1,-5} 未启动" -f $svc.Name, $svc.Port) -ForegroundColor DarkGray
        continue
    }

    # HTTP 探针：任何 HTTP 状态码都说明端口后有服务在应答
    $probe = 'n/a'
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $resp = Invoke-WebRequest -Uri $svc.Url -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        $sw.Stop()
        $probe = "HTTP $($resp.StatusCode) ($($sw.ElapsedMilliseconds)ms)"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) { $probe = "HTTP $code (有应答)" } else { $probe = "无应答: $($_.Exception.Message.Split([char]10)[0])" }
    }

    $color = if ($probe -match '^HTTP') { 'Green' } else { 'Yellow' }

    Write-Host ("  {0,-18} :{1,-5} PID {2,-7} {3,-6} [{4}]  {5}" -f `
        $svc.Name, $svc.Port, $owner.PID, $owner.Name, $owner.Origin, $probe) -ForegroundColor $color
}

Write-Host ''
Write-Host '==================== 登录链路快检 ====================' -ForegroundColor Cyan
try {
    $body = '{"username":"admin","password":"admin123"}'
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:80/dev-api/system/auth/login' -Method POST `
            -ContentType 'application/json' -Body $body -TimeoutSec 8 -UseBasicParsing -ErrorAction Stop
    $json = $r.Content | ConvertFrom-Json
    if ($json.data.access_token) {
        Write-Host "  登录 OK，已签发 token（前 8 位 $($json.data.access_token.Substring(0,8))...）" -ForegroundColor Green
    } else {
        Write-Host "  登录返回异常：$($r.Content)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  登录链路不通：$($_.Exception.Message.Split([char]10)[0])" -ForegroundColor DarkGray
    Write-Host '  （需要 80 前端 + 8889 网关 + 8000 system-service 三个都在跑）' -ForegroundColor DarkGray
}
Write-Host ''

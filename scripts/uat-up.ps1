# =====================================================================
# uat-up.ps1 —— 启动 UAT 环境（与 DEV 完全隔离的一套后端）
#
# 用法（仓库根目录执行）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-up.ps1            # 先构建再启动
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\uat-up.ps1 -SkipBuild # 跳过构建
#
# 启动的服务（全部用 jar 起独立进程，DEV 在 IDEA 里跑，互不干扰）：
#   system-service  8001  连 ry-cloud-uat、Redis db1
#   blog-service    8011  连 ry-cloud-uat、Redis db1
#   uat-service     8020  假登录/测试功能（DEV 环境不存在这个进程）
#   gateway         8890  转发 /uat/** -> 8020（GATEWAY_UAT_SERVICE 才开启该路由）
#
# 前端另行启动：cd web; yarn dev:uat（端口 81，代理到 8890）
#
# 前置条件：先跑过 scripts\init-uat-db.ps1 初始化 ry-cloud-uat 库。
# =====================================================================
param([switch]$SkipBuild)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$java = 'D:\Program\Environment\JDK\Jdk17\bin\java.exe'
$mvn  = 'D:\Program\Environment\Maven\apache-maven-3.6.3\bin\mvn.cmd'

# ---- 统一的环境变量（UAT 与 DEV 隔离的全部秘密就在这几行）----
$env:SYSTEM_PORT         = '8001'
$env:SYSTEM_DB_URL       = 'jdbc:mysql://localhost:3306/ry-cloud-uat?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:SYSTEM_REDIS_DATABASE = '1'

$env:BLOG_PORT           = '8011'
$env:BLOG_DB_URL         = $env:SYSTEM_DB_URL
$env:BLOG_REDIS_DATABASE = '1'

$env:UAT_PORT            = '8020'
$env:UAT_DB_URL          = $env:SYSTEM_DB_URL
$env:UAT_REDIS_DATABASE  = '1'

$env:GATEWAY_PORT        = '8890'
$env:GATEWAY_SYSTEM_SERVICE = 'http://localhost:8001'
$env:GATEWAY_BLOG_SERVICE   = 'http://localhost:8011'
$env:GATEWAY_UAT_SERVICE    = 'http://localhost:8020'

# ---- 构建 ----
if (-not $SkipBuild) {
    Write-Host '==> mvn install（可加 -SkipBuild 跳过）...' -ForegroundColor Cyan
    $env:JAVA_HOME = 'D:\Program\Environment\JDK\Jdk17'
    & $mvn -q -DskipTests install '-f' (Join-Path $repo 'backend\pom.xml')
    if ($LASTEXITCODE -ne 0) { throw 'Maven 构建失败' }
}

# ---- 启动 ----
$logDir = Join-Path $repo 'logs\uat'
New-Item -ItemType Directory -Force $logDir | Out-Null
$workDir = Join-Path $repo 'backend'   # 日志统一落 仓库根/logs（logback 相对 user.dir）

function Start-Java($jar, $name) {
    $out = Join-Path $logDir "$name.out.log"
    # 本机 Start-Process 会因 NO_PROXY/no_proxy 环境变量大小写冲突抛错，
    # 改用 .NET Process 启动 cmd 重定向；java 是 cmd 的子进程，
    # 独立于本脚本存活，退出由 uat-down.ps1 按端口回收
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    # 路径均无空格，无需引号（带引号的 cmd /c 有转义坑，刻意从简）
    $psi.Arguments = "/c $java -jar $jar > $out 2>&1"
    $psi.WorkingDirectory = $workDir
    $psi.WindowStyle = 'Hidden'
    $p = [System.Diagnostics.Process]::Start($psi)
    Write-Host ("  启动 {0,-18} (cmd PID {1})" -f $name, $p.Id)
    return $p.Id
}

Write-Host '==> 启动 UAT 服务...' -ForegroundColor Cyan
$services = @(
    @{ jar = 'system-service\target\system-service-1.0.jar'; name = 'system-service-8001'; port = 8001 },
    @{ jar = 'blog-service\target\blog-service-1.0.jar';     name = 'blog-service-8011';   port = 8011 },
    @{ jar = 'uat-service\target\uat-service-1.0.jar';       name = 'uat-service-8020';    port = 8020 },
    @{ jar = 'gateway-service\target\gateway-service-1.0.jar'; name = 'gateway-8890';      port = 8890 }
)
$pids = @()
foreach ($s in $services) {
    # 端口已在监听 → 跳过启动（典型场景：你已在 IDEA 里跑着 UatApplication 等实例）
    if (Get-NetTCPConnection -LocalPort $s.port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host ("  跳过 {0,-18} :{1} 已在监听（可能你已在 IDE 里启动）" -f $s.name, $s.port) -ForegroundColor Yellow
        continue
    }
    $pids += Start-Java (Join-Path $repo ("backend\" + $s.jar)) $s.name
}
$pids | Set-Content (Join-Path $logDir 'pids.txt')

# ---- 等待就绪 ----
function Wait-Port($port, $name, $timeoutSec = 90) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

Write-Host ''
Write-Host '==> 等待端口就绪...' -ForegroundColor Cyan
$allOk = $true
foreach ($pair in @(@(8001,'system-service'), @(8011,'blog-service'), @(8020,'uat-service'), @(8890,'gateway'))) {
    $ok = Wait-Port $pair[0] $pair[1]
    if ($ok) { Write-Host ("  {0,-16} :{1} 就绪" -f $pair[1], $pair[0]) -ForegroundColor Green }
    else { Write-Host ("  {0,-16} :{1} 未就绪（看 logs\uat\*.err.log）" -f $pair[1], $pair[0]) -ForegroundColor Red; $allOk = $false }
}

Write-Host ''
if ($allOk) {
    Write-Host '==> UAT 环境就绪。自检 /uat/status：' -ForegroundColor Green
    try {
        $r = Invoke-RestMethod -Uri 'http://127.0.0.1:8890/uat/status' -TimeoutSec 10
        Write-Host ("    db={0} redis={1} admin就绪={2} user就绪={3}" -f `
            $r.data.db, $r.data.redis, $r.data.accounts.admin.ready, $r.data.accounts.user.ready)
    } catch { Write-Warning "自检请求失败：$($_.Exception.Message)" }
    Write-Host ''
    Write-Host '前端：cd web; yarn dev:uat   （端口 81，自动开浏览器）' -ForegroundColor Yellow
    Write-Host '登录页有「UAT 快捷进入」按钮；顶栏横幅可随时切换 管理员/普通用户'
} else {
    Write-Host '!! 有服务未就绪，排查：logs\uat\ 下的 *.err.log / *.out.log' -ForegroundColor Red
}

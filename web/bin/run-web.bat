@echo off
REM 用途：以开发模式启动前端 Vite 服务（package.json 的 dev 脚本），支持热更新。
REM 说明：端口与接口代理在 web/vite.config.js 中配置（默认 80 端口，/dev-api 代理到后端网关）。
REM       需要 81 端口并连 UAT 网关时，请用 yarn dev:uat，不要用本脚本。
REM 前置：已执行 package.bat 安装依赖，否则 yarn dev 会因缺少 vite 而启动失败。
echo.
echo [信息] 使用 Vite 命令运行 Web 工程。
echo.

%~d0
cd %~dp0

cd ..
yarn dev

pause
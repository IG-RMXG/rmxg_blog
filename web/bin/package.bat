@echo off
REM 用途：安装前端依赖，生成 web/node_modules，首次拉取代码后执行一次即可。
REM 说明：yarn 显式指定 registry=https://registry.npmmirror.com（国内镜像）以加快下载。
REM 幂等性：可重复执行，已安装且版本未变的包会跳过，不会重复下载已缓存的依赖。
REM 前置：本机已安装 Node.js 与 yarn，且能访问上述镜像站。
echo.
echo [信息] 安装Web工程，生成node_modules文件。
echo.

%~d0
cd %~dp0

cd ..
yarn --registry=https://registry.npmmirror.com

pause
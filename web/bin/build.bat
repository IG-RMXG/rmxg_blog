@echo off
REM 用途：打包前端工程，执行 package.json 中的 build:prod（即 vite build），产物输出到 web/dist。
REM 前置：已安装 Node.js 与 yarn，且已执行 package.bat 完成依赖安装（存在 node_modules）。
REM 说明：%~d0 切到脚本所在盘符、cd %~dp0 切到脚本所在目录，再回到上一级的 web 工程根，
REM       这样从任意路径双击运行都能找到 package.json，不会出现 yarn 找不到工程的情况。
REM 末尾 pause 用于留住控制台窗口，方便查看构建日志与失败原因。
echo.
echo [信息] 打包Web工程，生成dist文件。
echo.

%~d0
cd %~dp0

cd ..
yarn build:prod

pause
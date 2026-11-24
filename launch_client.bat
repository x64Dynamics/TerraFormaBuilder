@echo off
title TerraFormaBuilder - Minecraft 1.20.1 Fabric Client
echo ========================================================
echo Launching TerraFormaBuilder Client (Minecraft 1.20.1 Fabric)
echo ========================================================
cd /d "%~dp0"
call modw.bat "-Pmc=1.20.1" "-Ploader=fabric" runClient
pause

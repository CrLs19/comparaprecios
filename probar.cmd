@echo off
setlocal
cd /d "%~dp0"
if not exist "evidencia-local" mkdir "evidencia-local"
echo Ejecutando pruebas. La primera vez puede tardar al descargar dependencias.
call mvnw.cmd -B -ntp clean verify > "evidencia-local\maven-verify.log" 2>&1
set "RESULTADO=%ERRORLEVEL%"
type "evidencia-local\maven-verify.log"
if "%RESULTADO%"=="0" (
  echo Pruebas terminadas correctamente.
  if exist "target\site\jacoco\index.html" start "" "target\site\jacoco\index.html"
) else (
  echo Fallo la ejecucion. Revisar evidencia-local\maven-verify.log
)
pause
exit /b %RESULTADO%

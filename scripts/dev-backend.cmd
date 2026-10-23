@echo off
REM One command for the backend. Forwards start | stop | status.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev-backend.ps1" %*

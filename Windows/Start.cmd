@echo off
setlocal
cd /d "%~dp0"
if exist "Game\Durka.exe" if exist "Game\Durka.pck" if exist "Game\data_Durotale_windows_x86_64\Durotale.dll" goto run
echo Preparing the game for its first launch...
powershell.exe -NoProfile -Command "$ErrorActionPreference='Stop'; $dest=Join-Path (Get-Location) 'Durka-Windows-x64.zip'; $out=[IO.File]::Create($dest); try { foreach($part in (Get-ChildItem -File 'payload/*.b64' | Sort-Object Name)) { $bytes=[Convert]::FromBase64String([IO.File]::ReadAllText($part.FullName)); $out.Write($bytes,0,$bytes.Length) } } finally { $out.Dispose() }; if((Get-FileHash -Algorithm SHA256 -LiteralPath $dest).Hash -ne 'a8f581e73cce0a99a6bab62be2d5f7a34e1c4da7a313e299c18746aa8e92ef3f'){throw 'Archive verification failed. Extract the entire download and try again.'}; Expand-Archive -LiteralPath $dest -DestinationPath 'Game.pending' -Force; Move-Item -LiteralPath 'Game.pending' -Destination 'Game'"
if errorlevel 1 goto failed
:run
start "" "%~dp0Game\Durka.exe"
exit /b 0
:failed
echo The game could not be unpacked. Please check free disk space and extract the whole ZIP into a fresh folder.
pause
exit /b 1

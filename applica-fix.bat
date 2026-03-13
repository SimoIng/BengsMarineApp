@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ╔═══════════════════════════════════════════════════════════╗
echo ║     🔧 APPLICAZIONE FIX: Report Multi-Ciclo             ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

REM Verifica di essere nella directory del progetto
if not exist "src\test\java\tests" (
    echo ❌ ERRORE: Directory src\test\java\tests non trovata!
    echo 💡 Esegui questo script dalla root del progetto BengsMarineApp
    echo.
    pause
    exit /b 1
)

echo 📂 Directory progetto: %CD%
echo.

REM Path del file da modificare
set "FILE_PATH=src\test\java\tests\LoginCambioColonninaAccensioneSpegnimentoTestsParametrized.java"

REM Verifica esistenza file originale
if not exist "%FILE_PATH%" (
    echo ❌ ERRORE: File %FILE_PATH% non trovato!
    pause
    exit /b 1
)

echo ✅ File trovato: %FILE_PATH%
echo.

REM Crea backup se non esiste già
set "BACKUP_PATH=%FILE_PATH%.backup-%date:~-4,4%%date:~-7,2%%date:~-10,2%-%time:~0,2%%time:~3,2%%time:~6,2%"
set "BACKUP_PATH=%BACKUP_PATH: =0%"

if exist "%FILE_PATH%.backup" (
    echo ℹ️  Backup già esistente, creo nuovo backup con timestamp
    copy "%FILE_PATH%" "%BACKUP_PATH%" >nul
    echo ✅ Backup creato: %BACKUP_PATH%
) else (
    copy "%FILE_PATH%" "%FILE_PATH%.backup" >nul
    echo ✅ Backup creato: %FILE_PATH%.backup
)
echo.

REM Verifica se il file con il fix esiste nella stessa directory dello script
set "FIX_FILE=%~dp0LoginCambioColonninaAccensioneSpegnimentoTestsParametrized.java"

if not exist "%FIX_FILE%" (
    echo ❌ ERRORE: File con il fix non trovato!
    echo 💡 Assicurati che LoginCambioColonninaAccensioneSpegnimentoTestsParametrized.java
    echo    si trovi nella stessa directory di questo script batch
    echo.
    pause
    exit /b 1
)

echo ✅ File con fix trovato: %FIX_FILE%
echo.

REM Chiedi conferma
echo ⚠️  ATTENZIONE: Questo script sostituirà il file esistente
echo.
echo 📄 File originale: %FILE_PATH%
echo 💾 Backup salvato in: %FILE_PATH%.backup
echo 🔧 Nuovo file (con fix): %FIX_FILE%
echo.
set /p CONFIRM="Vuoi procedere con la sostituzione? (S/N): "

if /i not "%CONFIRM%"=="S" (
    echo.
    echo ❌ Operazione annullata dall'utente
    echo.
    pause
    exit /b 0
)

echo.
echo 🔄 Applicazione del fix in corso...

REM Copia il nuovo file
copy /Y "%FIX_FILE%" "%FILE_PATH%" >nul

if %ERRORLEVEL% equ 0 (
    echo ✅ Fix applicato con successo!
    echo.
    echo ╔═══════════════════════════════════════════════════════════╗
    echo ║              ✅ OPERAZIONE COMPLETATA                    ║
    echo ╚═══════════════════════════════════════════════════════════╝
    echo.
    echo 📋 RIEPILOGO:
    echo    • File modificato: %FILE_PATH%
    echo    • Backup salvato: %FILE_PATH%.backup
    echo    • Modifiche applicate: ✅
    echo.
    echo 🚀 PROSSIMI PASSI:
    echo    1. Verifica che il file sia compilato correttamente
    echo    2. Esegui un test con numero.cicli=2 in test.properties
    echo    3. Verifica che vengano creati i backup dei cicli
    echo.
    echo 💡 Per maggiori dettagli consulta README-FIX-MULTI-CICLO.md
    echo.
) else (
    echo ❌ ERRORE durante la copia del file!
    echo 💡 Verifica i permessi di scrittura
    echo.
)

pause

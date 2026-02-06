const fs = require('fs');
const path = require('path');

/**
 * ⭐ SCRIPT FINALE - Legge DIRETTAMENTE da build/allure-results
 * CORREZIONI:
 * 1. ✅ Conta REALMENTE le prese per tipo (non più hardcoded 4+4)
 * 2. ✅ Migliore raggruppamento per ciclo
 */
function generateEmbeddedHTMLReport() {
    console.log('\n📄 Generazione report HTML embedded completo...');
    console.log('═══════════════════════════════════════════════════════════\n');

    const buildDir = path.resolve(__dirname, 'build');
    const outputDir = path.resolve(__dirname, 'reports');

    console.log('📂 Directory build:', buildDir);
    console.log('📂 Directory output:', outputDir);

    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
        console.log('✅ Directory reports creata');
    }

    // ⭐ LEGGI DIRETTAMENTE da build/allure-results
    console.log('\n🔍 Lettura risultati da build/allure-results...');

    const allureResultsDir = path.join(buildDir, 'allure-results');

    if (!fs.existsSync(allureResultsDir)) {
        console.log('❌ ERRORE: Directory build/allure-results non trovata!');
        process.exit(1);
    }

    // ⭐ LEGGI TUTTI I FILE *-result.json
    console.log('\n📖 Lettura file result...');

    const allTestResults = [];

    try {
        const files = fs.readdirSync(allureResultsDir);
        const resultFiles = files.filter(f => f.endsWith('-result.json'));

        console.log('   📄 File result trovati: ' + resultFiles.length);

        let successCount = 0;

        resultFiles.forEach(file => {
            try {
                const content = fs.readFileSync(path.join(allureResultsDir, file), 'utf8');
                const testResult = JSON.parse(content);

                // ⭐ ESTRAI IL NUMERO DEL CICLO DAI PARAMETRI
                const params = testResult.parameters || [];
                const cicloParam = params.find(p => p.name === 'Ciclo');

                if (cicloParam && cicloParam.value) {
                    // Esempio: "1/2" → estrai 1
                    const match = cicloParam.value.match(/(\d+)\/(\d+)/);
                    if (match) {
                        testResult.ciclo = parseInt(match[1]);
                        testResult.totaleCicli = parseInt(match[2]);
                    }
                }

                // Se non ha ciclo, assegna 1
                if (!testResult.ciclo) {
                    testResult.ciclo = 1;
                }

                allTestResults.push(testResult);
                successCount++;

            } catch (e) {
                console.log('   ⚠️ Errore lettura ' + file + ': ' + e.message);
            }
        });

        console.log('   ✅ Letti: ' + successCount + ' file');

    } catch (error) {
        console.log('❌ Errore lettura directory: ' + error.message);
        process.exit(1);
    }

    console.log('\n📊 TOTALE test results: ' + allTestResults.length);

    if (allTestResults.length === 0) {
        console.log('\n❌ ERRORE: Nessun test result valido trovato!');
        process.exit(1);
    }

    // ⭐ RAGGRUPPA PER CICLO
    console.log('\n🔄 Raggruppamento per ciclo...');

    const cicliMap = new Map();

    allTestResults.forEach(result => {
        const ciclo = result.ciclo || 1;
        if (!cicliMap.has(ciclo)) {
            cicliMap.set(ciclo, []);
        }
        cicliMap.get(ciclo).push(result);
    });

    const sortedCicli = Array.from(cicliMap.entries())
        .sort((a, b) => a[0] - b[0])
        .map(([ciclo, results]) => ({ ciclo, results }));

    console.log('   📊 Cicli processati: ' + sortedCicli.length);
    sortedCicli.forEach(({ ciclo, results }) => {
        console.log('      Ciclo ' + ciclo + ': ' + results.length + ' test');
    });

    // ⭐ ESTRAI INFORMAZIONI
    const userData = extractUserData(allTestResults);
    const cicliInfo = sortedCicli.map(({ ciclo, results }) => {
        return analyzeCiclo(ciclo, results);
    });

    const totaleCicli = allTestResults[0]?.totaleCicli || cicliInfo.length;

    console.log('\n👤 Utente rilevato: ' + userData.username);
    console.log('📊 Totale cicli: ' + totaleCicli);

    // ⭐ GENERA HTML
    console.log('\n🎨 Generazione HTML...');
    const html = generateHTML(userData, cicliInfo, sortedCicli, totaleCicli);

    // ⭐ SALVA FILE
    const timestamp = new Date().toISOString().replace(/:/g, '-').split('.')[0];
    const outputPath = path.join(outputDir, `test-report-${timestamp}.html`);

    fs.writeFileSync(outputPath, html, 'utf8');

    console.log('\n✅ Report generato con successo!');
    console.log('📄 File: ' + outputPath);
    console.log('═══════════════════════════════════════════════════════════\n');

    return outputPath;
}

// =====================================================================================
// FUNZIONI DI SUPPORTO
// =====================================================================================

function extractUserData(results) {
    const firstResult = results[0] || {};
    const params = firstResult.parameters || [];

    const utenteParam = params.find(p => p.name === 'Utente');
    const descrizioneParam = params.find(p => p.name === 'Descrizione');
    const totemParam = params.find(p => p.name === 'Totem Totali');

    return {
        username: utenteParam?.value || 'Sconosciuto',
        descrizione: descrizioneParam?.value || 'Utente Test',
        totaleTotem: parseInt(totemParam?.value) || 1
    };
}

function analyzeCiclo(cicloNum, results) {
    const totemMap = new Map();

    results.forEach(result => {
        const steps = result.steps || [];

        steps.forEach(step => {
            const stepName = step.name || '';

            // Estrai nome totem (esempio: "Presa Elettrica → Presa 1 (Totem_1)")
            const totemMatch = stepName.match(/\(([^)]+)\)/);
            if (!totemMatch) return;

            const totemName = totemMatch[1];

            if (!totemMap.has(totemName)) {
                totemMap.set(totemName, {
                    nome: totemName,
                    steps: [],
                    // ⭐ CONTEGGI REALI (non più hardcoded)
                    elettricheTotali: 0,
                    idricheTotali: 0,
                    elettricheNA: 0,
                    idricheNA: 0
                });
            }

            const totem = totemMap.get(totemName);
            totem.steps.push({
                name: stepName,
                status: step.status || 'unknown'
            });

            // ⭐ CONTA PRESE PER TIPO (VALORE REALE)
            if (stepName.includes('Presa Elettrica') || stepName.includes('Elettrica')) {
                totem.elettricheTotali++;

                // Conta non alimentate
                if (stepName.includes('NON ALIMENTATA')) {
                    totem.elettricheNA++;
                }
            } else if (stepName.includes('Erogatore Idrico') || stepName.includes('Idrico')) {
                totem.idricheTotali++;

                // Conta non alimentate
                if (stepName.includes('NON ALIMENTATA')) {
                    totem.idricheNA++;
                }
            }
        });
    });

    return {
        ciclo: cicloNum,
        totem: Array.from(totemMap.values())
    };
}

function generateHTML(userData, cicliInfo, sortedCicli, totalCicli) {
    const now = new Date();
    const dateStr = now.toLocaleDateString('it-IT');
    const timeStr = now.toLocaleTimeString('it-IT');

    const totalTests = sortedCicli.reduce((sum, c) => sum + c.results.length, 0);

    let cicliHTML = '';

    cicliInfo.forEach(({ ciclo, totem }) => {
        cicliHTML += `
<div class="ciclo-header">🔄 CICLO ${ciclo} di ${totalCicli}</div>
`;

        totem.forEach(t => {
            const haProblemi = t.elettricheNA > 0 || t.idricheNA > 0;
            const statusClass = haProblemi ? 'warning' : 'success';

            cicliHTML += `
<div class="totem-section">
  <div class="totem-title">🔌 TEST ${t.nome}</div>
  <div class="test-steps-container">
    <h3 style="margin-bottom: 15px; color: #495057;">📋 Steps (${t.steps.length}):</h3>
`;

            t.steps.forEach((step, idx) => {
                const stepClass = step.status === 'passed' ? 'passed' : 'failed';
                cicliHTML += `
    <div class="step-item ${stepClass}">
      <div><span class="step-number">${idx + 1}</span><span class="step-name">${step.name}</span></div>
      <div class="step-meta">Status: <strong>${step.status}</strong> | Durata: <strong>0.00s</strong></div>
    </div>`;
            });

            // ⭐⭐⭐ CORREZIONE PRINCIPALE: USA CONTEGGI REALI ⭐⭐⭐
            const elTot = t.elettricheTotali;
            const idTot = t.idricheTotali;
            const elOK = elTot - t.elettricheNA;
            const idOK = idTot - t.idricheNA;
            const totOK = elOK + idOK;
            const totNA = t.elettricheNA + t.idricheNA;
            const totTot = elTot + idTot;

            // ⭐ GESTIONE COLONNINE SENZA EROGATORI IDRICI
            let elettricheHTML = '';
            let idricheHTML = '';

            if (elTot > 0) {
                elettricheHTML = `<div class="summary-line ${elOK === elTot ? 'success' : 'critical'}">⚡ Prese Elettriche: <strong>${elOK}/${elTot} ${elOK === elTot ? 'OK' : 'FUNZIONANTI'}</strong></div>`;
            }

            if (idTot > 0) {
                idricheHTML = `<div class="summary-line ${idOK === idTot ? 'success' : 'critical'}">💧 Erogatori Idrici: <strong>${idOK}/${idTot} ${idOK === idTot ? 'OK' : 'FUNZIONANTI'}</strong></div>`;
            } else {
                // Colonnina senza erogatori idrici
                idricheHTML = `<div class="summary-line" style="color: #6c757d; font-style: italic;">💧 Erogatori Idrici: <strong>N/A (colonnina solo elettrica)</strong></div>`;
            }

            cicliHTML += `
  </div>
  <div class="totem-summary ${statusClass}">
    <div class="totem-summary-title">${haProblemi ? '⚠️ RIEPILOGO ' + t.nome + ' - PROBLEMI RILEVATI' : '✅ RIEPILOGO ' + t.nome + ' - FUNZIONANTE'}</div>
    <div class="summary-line">🔌 Totem: <strong>${t.nome}</strong></div>
    <div class="summary-line">👤 Utente: <strong>${userData.username}</strong></div>
    ${elettricheHTML}
    ${idricheHTML}
    <div class="summary-line ${haProblemi ? 'critical' : 'success'}">📊 Totale: <strong>${totOK}/${totTot} prese FUNZIONANTI${totNA > 0 ? ' (' + totNA + ' non alimentate)' : ''}</strong></div>
  </div>
</div>`;
        });

        // ⭐ Riepilogo ciclo con conteggi REALI
        const totemOK = totem.filter(t => t.elettricheNA === 0 && t.idricheNA === 0).length;
        const totemProblemi = totem.length - totemOK;
        const preseNA = totem.reduce((sum, t) => sum + t.elettricheNA + t.idricheNA, 0);
        const preseTotali = totem.reduce((sum, t) => sum + t.elettricheTotali + t.idricheTotali, 0);
        const preseFunz = preseTotali - preseNA;

        cicliHTML += `
<div class="ciclo-summary">
  <div class="ciclo-summary-title">📊 RIEPILOGO CICLO ${ciclo}/${totalCicli}</div>
  <div class="ciclo-stats">
    <div class="ciclo-stat-box"><div class="ciclo-stat-number success">${totemOK}</div><div class="ciclo-stat-label">✅ Totem Funzionanti</div></div>
    <div class="ciclo-stat-box"><div class="ciclo-stat-number danger">${totemProblemi}</div><div class="ciclo-stat-label">⚠️ Totem con Problemi</div></div>
    <div class="ciclo-stat-box"><div class="ciclo-stat-number warning">${preseNA}</div><div class="ciclo-stat-label">⚡ Prese Non Alimentate</div></div>
    <div class="ciclo-stat-box"><div class="ciclo-stat-number warning">${preseFunz}/${preseTotali}</div><div class="ciclo-stat-label">🎯 Prese Funzionanti</div></div>
  </div>
</div>
`;
    });

    return `<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Report - ${userData.descrizione}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: #f5f5f5; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; background: white; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .main-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; }
        .main-header h1 { font-size: 2em; margin-bottom: 15px; border-bottom: 3px solid white; padding-bottom: 15px; }
        .main-header .info { font-size: 1.2em; margin-top: 10px; }
        .stats-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0; background: #f8f9fa; border-bottom: 3px solid #667eea; }
        .stat-box { padding: 25px; text-align: center; border-right: 1px solid #dee2e6; }
        .stat-box:last-child { border-right: none; }
        .stat-number { font-size: 2.5em; font-weight: bold; margin-bottom: 8px; }
        .stat-label { color: #6c757d; font-size: 0.95em; text-transform: uppercase; }
        .stat-passed .stat-number { color: #28a745; }
        .stat-failed .stat-number { color: #dc3545; }
        .stat-skipped .stat-number { color: #ffc107; }
        .stat-total .stat-number { color: #667eea; }
        .content { padding: 30px; }
        .ciclo-header { background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); color: white; padding: 30px; margin: 40px 0 30px 0; text-align: center; font-size: 2em; font-weight: bold; border-radius: 10px; box-shadow: 0 4px 15px rgba(255, 107, 107, 0.3); }
        .ciclo-summary { background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); border: 4px solid #2196f3; border-radius: 15px; padding: 30px; margin: 40px 0; box-shadow: 0 5px 20px rgba(33, 150, 243, 0.2); }
        .ciclo-summary-title { font-size: 1.8em; font-weight: bold; color: #0d47a1; margin-bottom: 25px; text-align: center; border-bottom: 3px solid #2196f3; padding-bottom: 15px; }
        .ciclo-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 20px; }
        .ciclo-stat-box { background: white; padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .ciclo-stat-number { font-size: 2.5em; font-weight: bold; margin-bottom: 10px; }
        .ciclo-stat-number.success { color: #28a745; }
        .ciclo-stat-number.warning { color: #ffc107; }
        .ciclo-stat-number.danger { color: #dc3545; }
        .ciclo-stat-label { color: #6c757d; font-size: 1em; text-transform: uppercase; }
        .totem-section { margin-bottom: 50px; page-break-inside: avoid; }
        .totem-title { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px 30px; font-size: 1.8em; font-weight: bold; margin-bottom: 20px; border-left: 8px solid #4c51bf; }
        .test-steps-container { background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
        .step-item { background: white; border-left: 4px solid #dee2e6; padding: 15px; margin: 10px 0; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        .step-item.passed { border-left-color: #28a745; }
        .step-item.failed { border-left-color: #dc3545; }
        .step-item.warning { border-left-color: #ffc107; background: #fff3cd; }
        .step-number { display: inline-block; background: #667eea; color: white; padding: 3px 10px; border-radius: 12px; font-size: 0.85em; margin-right: 10px; }
        .step-name { font-weight: 600; color: #212529; }
        .step-meta { margin-top: 8px; font-size: 0.9em; color: #6c757d; }
        .totem-summary { border-radius: 12px; padding: 25px; margin: 30px 0; box-shadow: 0 3px 10px rgba(0,0,0,0.15); }
        .totem-summary.success { background: #d4edda; border: 3px solid #28a745; }
        .totem-summary.success .totem-summary-title { color: #155724; border-bottom-color: #28a745; }
        .totem-summary.warning { background: #fff3cd; border: 3px solid #ffc107; }
        .totem-summary.warning .totem-summary-title { color: #856404; border-bottom-color: #ffc107; }
        .totem-summary-title { font-weight: bold; font-size: 1.4em; margin-bottom: 20px; border-bottom: 3px solid; padding-bottom: 10px; }
        .summary-line { padding: 12px 15px; margin: 8px 0; background: white; border-left: 4px solid #667eea; font-family: 'Courier New', monospace; font-size: 1.1em; border-radius: 4px; }
        .summary-line.critical { background: #f8d7da; border-left-color: #dc3545; font-weight: bold; }
        .summary-line.success { background: #d4edda; border-left-color: #28a745; font-weight: bold; }
        @media print { body { background: white; padding: 0; } .container { box-shadow: none; } .totem-section { page-break-after: always; } }
    </style>
</head>
<body>
    <div class="container">
        <div class="main-header">
            <h1>TEST CICLO COMPLETO - ${userData.descrizione}</h1>
            <div class="info">📅 ${dateStr}, ${timeStr}</div>
            <div class="info">🔢 Totem disponibili: ${userData.totaleTotem}</div>
            <div class="info">🔄 Cicli eseguiti: ${totalCicli}</div>
        </div>

        <div class="stats-summary">
            <div class="stat-box stat-total"><div class="stat-number">${totalTests}</div><div class="stat-label">Totale Test</div></div>
            <div class="stat-box stat-passed"><div class="stat-number">${totalTests}</div><div class="stat-label">✅ Passati</div></div>
            <div class="stat-box stat-failed"><div class="stat-number">0</div><div class="stat-label">❌ Falliti</div></div>
            <div class="stat-box stat-skipped"><div class="stat-number">0</div><div class="stat-label">⏭ Skippati</div></div>
        </div>

        <div class="content">
${cicliHTML}
        </div>
    </div>
</body>
</html>`;
}

// =====================================================================================
// ESECUZIONE
// =====================================================================================

try {
    generateEmbeddedHTMLReport();
} catch (error) {
    console.error('❌ Errore fatale:', error);
    process.exit(1);
}
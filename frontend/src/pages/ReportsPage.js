import { renderSidebar } from '../components/Sidebar.js';
import { reportsApi } from '../api/reports.js';
import { showToast } from '../components/Toast.js';
import { transactionsApi } from '../api/transactions.js';

export function ReportsPage() {
    return `
        ${renderSidebar('/reports')}
        <main class="app-content" id="reports-page">
            <div class="page-header" style="display:flex;justify-content:space-between;align-items:flex-end;gap:var(--space-4);flex-wrap:wrap;">
                <div>
                    <h1>Relatórios <span class="accent">Financeiros</span></h1>
                    <p>DRE, Fluxo de Caixa Projetado e Análise de Impacto</p>
                </div>
                <div style="display:flex;align-items:center;gap:var(--space-2);flex-wrap:wrap;">
                    <select id="export-month-select" class="input select" style="width:auto;margin:0;font-size:0.85rem;padding:8px 12px;background:var(--card-bg);border:1px solid var(--border-color);color:var(--text-primary);border-radius:var(--radius-md);">
                        <!-- preenchido via JS -->
                    </select>
                    <button id="btn-export-excel" class="btn btn-secondary" style="font-size:0.85rem;padding:8px 16px;white-space:nowrap;">
                        <i class="fas fa-file-excel"></i> Exportar Excel
                    </button>
                    <button id="btn-export-pdf" class="btn btn-primary" style="font-size:0.85rem;padding:8px 16px;white-space:nowrap;">
                        <i class="fas fa-file-pdf"></i> Exportar PDF
                    </button>
                </div>
            </div>
            <div class="report-tabs" id="report-tabs">
                <button class="report-tab active" data-tab="dre"><i class="fas fa-file-invoice-dollar"></i> DRE</button>
                <button class="report-tab" data-tab="cashflow"><i class="fas fa-water"></i> Fluxo de Caixa</button>
                <button class="report-tab" data-tab="whatif"><i class="fas fa-flask"></i> What-If</button>
            </div>
            <div id="report-content" class="report-content">
                <div class="loading-placeholder" style="padding:3rem;text-align:center;color:var(--text-muted);"><i class="fas fa-spinner fa-spin"></i> Carregando...</div>
            </div>
        </main>
    `;
}

let dreData = null, cashData = null, dreChart = null, cfChart = null, whatIfChart = null;

export async function initReportsPage() {
    // Popula o seletor de meses para a exportação
    populateMonthSelect();

    document.querySelectorAll('.report-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.report-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            renderTab(tab.dataset.tab);
        });
    });
    document.getElementById('btn-export-pdf')?.addEventListener('click', exportPDF);
    document.getElementById('btn-export-excel')?.addEventListener('click', exportExcel);
    await renderTab('dre');
}

async function renderTab(tab) {
    const c = document.getElementById('report-content');
    if (!c) return;
    if (tab === 'dre') { await renderDRE(c); }
    else if (tab === 'cashflow') { await renderCashFlow(c); }
    else { renderWhatIf(c); }
}

function fmtCurrency(v) {
    const n = parseFloat(v) || 0;
    return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function dreRow(label, value, cls = '') {
    const v = parseFloat(value) || 0;
    const color = v < 0 ? 'color:var(--accent-danger)' : '';
    return `<tr class="${cls}"><td>${label}</td><td style="text-align:right;font-family:var(--font-mono);${color}">${fmtCurrency(v)}</td></tr>`;
}

async function renderDRE(container) {
    try {
        if (!dreData) dreData = await reportsApi.getDre();
    } catch(e) { console.error(e); }
    const d = dreData || {};
    const periodLabel = d.periodoLabel || 'Último semestre';

    container.innerHTML = `
        <div class="card" style="padding:var(--space-6);margin-bottom:var(--space-4);">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:var(--space-4);">
                <h3 style="color:var(--accent-info);margin:0;"><i class="fas fa-file-invoice-dollar" style="margin-right:8px;"></i>DRE — Demonstração do Resultado</h3>
                <span style="color:var(--text-secondary);font-size:0.85rem;">${periodLabel}</span>
            </div>
            <div class="table-responsive">
                <table class="data-table dre-table" id="dre-table">
                    <thead><tr><th>Conta</th><th style="text-align:right;">Valor (R$)</th></tr></thead>
                    <tbody>
                        ${dreRow('Receita Bruta', d.receitaBruta, 'dre-header')}
                        ${dreRow('(+) Outras Receitas', d.outrasReceitas)}
                        ${dreRow('(-) Deduções sobre Receita', d.deducoesSobreReceita)}
                        ${dreRow('= Receita Líquida', d.receitaLiquida, 'dre-subtotal')}
                        ${dreRow('(-) Custo das Mercadorias/Serviços', d.custoVariavel)}
                        ${dreRow('= Lucro Bruto', d.lucroBruto, 'dre-subtotal')}
                        <tr class="dre-margin"><td>Margem Bruta</td><td style="text-align:right;font-family:var(--font-mono);">${(d.margemBruta||0).toFixed?.(1) || '0.0'}%</td></tr>
                        ${dreRow('(-) Despesas Fixas / Administrativas', d.despesasFixas)}
                        ${dreRow('(-) Depreciação', d.depreciacao)}
                        ${dreRow('(-) Despesas Financeiras', d.despesasFinanceiras)}
                        ${dreRow('(-) Outras Despesas', d.outrasDespesas)}
                        ${dreRow('= Total Despesas Operacionais', d.totalDespesasOperacionais, 'dre-subtotal')}
                        ${dreRow('= Lucro Operacional (EBIT)', d.lucroOperacional, 'dre-subtotal')}
                        <tr class="dre-margin"><td>Margem Operacional</td><td style="text-align:right;font-family:var(--font-mono);">${(d.margemOperacional||0).toFixed?.(1) || '0.0'}%</td></tr>
                        ${dreRow('(-) Impostos sobre Lucro (IR+CSLL)', d.impostosSobreLucro)}
                        ${dreRow('= LUCRO LÍQUIDO', d.lucroLiquido, 'dre-total')}
                        <tr class="dre-margin dre-total-margin"><td>Margem Líquida</td><td style="text-align:right;font-family:var(--font-mono);font-weight:700;">${(d.margemLiquida||0).toFixed?.(1) || '0.0'}%</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="card" style="padding:var(--space-6);">
            <h3 style="color:var(--accent-info);font-size:1rem;margin-bottom:var(--space-4);"><i class="fas fa-chart-bar" style="margin-right:8px;"></i>Evolução Mensal</h3>
            <div style="position:relative;height:300px;width:100%;"><canvas id="dre-chart"></canvas></div>
        </div>
    `;
    renderDreChart(d.evolucaoMensal || []);
}

function renderDreChart(data) {
    const canvas = document.getElementById('dre-chart');
    if (!canvas || !window.Chart || data.length === 0) return;
    if (dreChart) dreChart.destroy();
    dreChart = new Chart(canvas.getContext('2d'), {
        type: 'bar',
        data: {
            labels: data.map(d => d.mes),
            datasets: [
                { label: 'Receita', data: data.map(d => d.receita), backgroundColor: 'rgba(59,130,246,0.7)', borderRadius: 4 },
                { label: 'Custos', data: data.map(d => d.custos), backgroundColor: 'rgba(239,68,68,0.7)', borderRadius: 4 },
                { label: 'Lucro', data: data.map(d => d.lucro), backgroundColor: 'rgba(16,185,129,0.7)', borderRadius: 4 }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: '#b0bec5' } }, tooltip: { backgroundColor: 'rgba(15,23,42,0.95)', titleColor: '#fff', bodyColor: '#b0bec5' } },
            scales: { x: { ticks: { color: '#78909c' }, grid: { display: false } }, y: { ticks: { color: '#78909c', callback: v => (v/1000).toFixed(0)+'k' }, grid: { color: 'rgba(255,255,255,0.04)' } } }
        }
    });
}

async function renderCashFlow(container) {
    try { if (!cashData) cashData = await reportsApi.getCashFlow(); } catch(e) { console.error(e); }
    const d = cashData || {};
    const all = [...(d.historico||[]), ...(d.projetado||[])];
    const histLen = (d.historico||[]).length;

    let tableRows = '';
    all.forEach((m, i) => {
        const isProj = i >= histLen;
        const riskCls = m.risco ? 'style="color:var(--accent-danger);font-weight:600;"' : '';
        tableRows += `<tr${isProj ? ' class="cf-projected"' : ''}>
            <td>${m.mes}${isProj ? ' <span class="badge badge-info" style="font-size:0.6rem;padding:1px 4px;">Proj</span>' : ''}</td>
            <td style="text-align:right;font-family:var(--font-mono);color:var(--accent-secondary);">${fmtCurrency(m.entradas)}</td>
            <td style="text-align:right;font-family:var(--font-mono);color:var(--accent-danger);">${fmtCurrency(m.saidas)}</td>
            <td style="text-align:right;font-family:var(--font-mono);">${fmtCurrency(m.saldo)}</td>
            <td style="text-align:right;font-family:var(--font-mono);" ${riskCls}>${fmtCurrency(m.acumulado)}</td>
        </tr>`;
    });

    const riskWarning = (d.mesesDeRisco||[]).length > 0
        ? `<div style="padding:10px 14px;background:var(--accent-danger-muted);border-left:4px solid var(--accent-danger);border-radius:4px;margin-bottom:var(--space-4);font-size:0.85rem;color:var(--accent-danger);"><i class="fas fa-exclamation-triangle" style="margin-right:6px;"></i><strong>Alerta:</strong> Saldo negativo projetado em: ${d.mesesDeRisco.join(', ')}</div>`
        : '';

    container.innerHTML = `
        ${riskWarning}
        <div class="card" style="padding:var(--space-6);margin-bottom:var(--space-4);">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:var(--space-4);">
                <h3 style="color:var(--accent-info);margin:0;"><i class="fas fa-water" style="margin-right:8px;"></i>Fluxo de Caixa — Histórico + Projeção</h3>
                <span style="font-size:0.85rem;color:var(--text-secondary);">Tendência: <strong style="color:${d.tendencia==='CRESCENTE'?'var(--accent-secondary)':d.tendencia==='DECRESCENTE'?'var(--accent-danger)':'var(--accent-warning)'};">${d.tendencia||'—'}</strong></span>
            </div>
            <div style="position:relative;height:320px;width:100%;"><canvas id="cf-chart"></canvas></div>
        </div>
        <div class="card" style="padding:var(--space-6);">
            <h3 style="color:var(--accent-info);font-size:1rem;margin-bottom:var(--space-4);"><i class="fas fa-table" style="margin-right:8px;"></i>Detalhamento Mensal</h3>
            <div class="table-responsive">
                <table class="data-table"><thead><tr><th>Mês</th><th style="text-align:right;">Entradas</th><th style="text-align:right;">Saídas</th><th style="text-align:right;">Saldo</th><th style="text-align:right;">Acumulado</th></tr></thead>
                <tbody>${tableRows}</tbody></table>
            </div>
        </div>
    `;
    renderCfChart(all, histLen);
}

function renderCfChart(all, histLen) {
    const canvas = document.getElementById('cf-chart');
    if (!canvas || !window.Chart || all.length === 0) return;
    if (cfChart) cfChart.destroy();

    const labels = all.map(m => m.mes);
    const entradas = all.map(m => parseFloat(m.entradas)||0);
    const saidas = all.map(m => -(parseFloat(m.saidas)||0));
    const acumulado = all.map(m => parseFloat(m.acumulado)||0);

    cfChart = new Chart(canvas.getContext('2d'), {
        type: 'bar',
        data: {
            labels,
            datasets: [
                { label: 'Entradas', data: entradas, backgroundColor: all.map((_,i) => i<histLen?'rgba(16,185,129,0.7)':'rgba(16,185,129,0.35)'), borderRadius: 4 },
                { label: 'Saídas', data: saidas, backgroundColor: all.map((_,i) => i<histLen?'rgba(239,68,68,0.7)':'rgba(239,68,68,0.35)'), borderRadius: 4 },
                { label: 'Acumulado', data: acumulado, type: 'line', borderColor: '#7c4dff', backgroundColor: 'transparent', borderWidth: 2, pointRadius: 3, pointBackgroundColor: '#7c4dff', tension: 0.3, yAxisID: 'y1' }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { labels: { color: '#b0bec5' } }, tooltip: { backgroundColor: 'rgba(15,23,42,0.95)', titleColor: '#fff', bodyColor: '#b0bec5' } },
            scales: {
                x: { ticks: { color: '#78909c' }, grid: { display: false } },
                y: { ticks: { color: '#78909c', callback: v => (v/1000).toFixed(0)+'k' }, grid: { color: 'rgba(255,255,255,0.04)' } },
                y1: { position: 'right', ticks: { color: '#7c4dff', callback: v => (v/1000).toFixed(0)+'k' }, grid: { display: false } }
            }
        }
    });
}

function renderWhatIf(container) {
    container.innerHTML = `
        <div class="card" style="padding:var(--space-6);margin-bottom:var(--space-4);">
            <h3 style="color:var(--accent-info);margin-bottom:var(--space-4);"><i class="fas fa-flask" style="margin-right:8px;"></i>Análise de Impacto (What-If)</h3>
            <p style="color:var(--text-secondary);font-size:0.85rem;margin-bottom:var(--space-6);">Simule mudanças nos seus indicadores e veja o impacto no resultado.</p>
            <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:var(--space-6);">
                <div>
                    <label style="font-size:0.8rem;color:var(--text-secondary);text-transform:uppercase;letter-spacing:0.05em;">Variação de Receita</label>
                    <div style="display:flex;align-items:center;gap:var(--space-3);margin-top:var(--space-2);">
                        <input type="range" id="wi-revenue" min="-50" max="50" value="0" class="sim-range cyan" style="flex:1;">
                        <span id="wi-revenue-val" style="font-family:var(--font-mono);color:#06b6d4;min-width:45px;text-align:right;">0%</span>
                    </div>
                </div>
                <div>
                    <label style="font-size:0.8rem;color:var(--text-secondary);text-transform:uppercase;letter-spacing:0.05em;">Variação de Custos</label>
                    <div style="display:flex;align-items:center;gap:var(--space-3);margin-top:var(--space-2);">
                        <input type="range" id="wi-cost" min="-50" max="50" value="0" class="sim-range pink" style="flex:1;">
                        <span id="wi-cost-val" style="font-family:var(--font-mono);color:#ec4899;min-width:45px;text-align:right;">0%</span>
                    </div>
                </div>
                <div>
                    <label style="font-size:0.8rem;color:var(--text-secondary);text-transform:uppercase;letter-spacing:0.05em;">Novo Investimento (R$)</label>
                    <div style="display:flex;align-items:center;gap:var(--space-3);margin-top:var(--space-2);">
                        <input type="range" id="wi-invest" min="0" max="100000" step="1000" value="0" class="sim-range purple" style="flex:1;">
                        <span id="wi-invest-val" style="font-family:var(--font-mono);color:#a855f7;min-width:70px;text-align:right;">R$ 0</span>
                    </div>
                </div>
            </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:var(--space-4);margin-bottom:var(--space-4);">
            <div class="card" style="padding:var(--space-5);text-align:center;">
                <h4 style="font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-secondary);margin-bottom:var(--space-2);">Cenário Atual</h4>
                <div id="wi-current-profit" style="font-family:var(--font-mono);font-size:1.8rem;font-weight:700;color:var(--accent-secondary);">R$ 0</div>
                <span style="font-size:0.75rem;color:var(--text-muted);">Lucro Líquido</span>
            </div>
            <div class="card" style="padding:var(--space-5);text-align:center;">
                <h4 style="font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;color:var(--text-secondary);margin-bottom:var(--space-2);">Cenário Simulado</h4>
                <div id="wi-simulated-profit" style="font-family:var(--font-mono);font-size:1.8rem;font-weight:700;color:#7c4dff;">R$ 0</div>
                <span id="wi-delta" style="font-size:0.75rem;color:var(--text-muted);">Diferença: R$ 0</span>
            </div>
        </div>
        <div class="card" style="padding:var(--space-6);">
            <div style="position:relative;height:280px;width:100%;"><canvas id="wi-chart"></canvas></div>
        </div>
    `;
    setupWhatIf();
}

function setupWhatIf() {
    const d = dreData || {};
    const baseReceita = parseFloat(d.receitaBruta) || 180000;
    const baseCustos = (parseFloat(d.custoVariavel)||0) + (parseFloat(d.totalDespesasOperacionais)||0) || 115000;
    const baseImpostos = parseFloat(d.impostosSobreLucro) || 13000;
    const baseLucro = parseFloat(d.lucroLiquido) || 41724;

    document.getElementById('wi-current-profit').textContent = fmtCurrency(baseLucro);

    const update = () => {
        const revDelta = parseInt(document.getElementById('wi-revenue').value);
        const costDelta = parseInt(document.getElementById('wi-cost').value);
        const invest = parseInt(document.getElementById('wi-invest').value);

        document.getElementById('wi-revenue-val').textContent = (revDelta>=0?'+':'')+revDelta+'%';
        document.getElementById('wi-cost-val').textContent = (costDelta>=0?'+':'')+costDelta+'%';
        document.getElementById('wi-invest-val').textContent = 'R$ '+(invest/1000).toFixed(0)+'k';

        const newReceita = baseReceita * (1 + revDelta/100);
        const newCustos = baseCustos * (1 + costDelta/100) + invest;
        const newLucro = newReceita - newCustos - baseImpostos;
        const delta = newLucro - baseLucro;

        document.getElementById('wi-simulated-profit').textContent = fmtCurrency(newLucro);
        document.getElementById('wi-simulated-profit').style.color = newLucro >= 0 ? '#7c4dff' : 'var(--accent-danger)';

        const deltaEl = document.getElementById('wi-delta');
        deltaEl.textContent = `Diferença: ${delta>=0?'+':''}${fmtCurrency(delta)}`;
        deltaEl.style.color = delta >= 0 ? 'var(--accent-secondary)' : 'var(--accent-danger)';

        renderWhatIfChart(baseReceita, baseCustos, baseLucro, newReceita, newCustos, newLucro);
    };

    ['wi-revenue','wi-cost','wi-invest'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', update);
    });
    update();
}

function renderWhatIfChart(baseR, baseC, baseL, newR, newC, newL) {
    const canvas = document.getElementById('wi-chart');
    if (!canvas || !window.Chart) return;
    if (whatIfChart) whatIfChart.destroy();
    whatIfChart = new Chart(canvas.getContext('2d'), {
        type: 'bar',
        data: {
            labels: ['Receita', 'Custos Totais', 'Lucro Líquido'],
            datasets: [
                { label: 'Atual', data: [baseR, baseC, baseL], backgroundColor: 'rgba(59,130,246,0.7)', borderRadius: 6 },
                { label: 'Simulado', data: [newR, newC, newL], backgroundColor: 'rgba(124,77,255,0.7)', borderRadius: 6 }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false, indexAxis: 'x',
            plugins: { legend: { labels: { color: '#b0bec5' } }, tooltip: { backgroundColor: 'rgba(15,23,42,0.95)' } },
            scales: { x: { ticks: { color: '#78909c' }, grid: { display: false } }, y: { ticks: { color: '#78909c', callback: v => (v/1000).toFixed(0)+'k' }, grid: { color: 'rgba(255,255,255,0.04)' } } }
        }
    });
}

async function exportPDF() {
    const btn = document.getElementById('btn-export-pdf');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Gerando...';
    btn.disabled = true;
    try {
        const { default: html2canvas } = await import('html2canvas');
        const { jsPDF } = await import('jspdf');
        const content = document.getElementById('report-content');
        const canvas = await html2canvas(content, { backgroundColor: '#0f172a', scale: 2 });
        const imgData = canvas.toDataURL('image/png');
        const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
        const pageW = pdf.internal.pageSize.getWidth();
        const pageH = pdf.internal.pageSize.getHeight();
        const margin = 10;
        const imgW = pageW - margin * 2;
        const imgH = (canvas.height * imgW) / canvas.width;
        let y = margin;
        // Header
        pdf.setFillColor(15, 23, 42);
        pdf.rect(0, 0, pageW, pageH, 'F');
        pdf.setTextColor(241, 245, 249);
        pdf.setFontSize(16);
        pdf.text('FinPred — Relatório Financeiro', margin, y + 6);
        pdf.setFontSize(9);
        pdf.setTextColor(148, 163, 184);
        pdf.text('Gerado em: ' + new Date().toLocaleDateString('pt-BR'), margin, y + 12);
        y += 18;
        // Content - split into pages if needed
        let remainingH = imgH;
        let srcY = 0;
        const availH = pageH - y - margin;
        while (remainingH > 0) {
            const sliceH = Math.min(remainingH, availH);
            const sliceCanvasH = (sliceH / imgH) * canvas.height;
            pdf.addImage(imgData, 'PNG', margin, y, imgW, imgH, undefined, 'FAST', 0);
            remainingH -= sliceH;
            if (remainingH > 0) { pdf.addPage(); y = margin; }
            else break;
        }
        pdf.save('finpred-relatorio.pdf');
        showToast('PDF gerado com sucesso!', 'success');
    } catch(e) {
        console.error('Erro ao gerar PDF:', e);
        showToast('Erro ao gerar PDF.', 'error');
    } finally {
        btn.innerHTML = '<i class="fas fa-file-pdf"></i> Exportar PDF';
        btn.disabled = false;
    }
}

function populateMonthSelect() {
    const monthSelect = document.getElementById('export-month-select');
    if (!monthSelect) return;
    monthSelect.innerHTML = '';
    const now = new Date();
    for (let i = 0; i < 12; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const val = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        const label = d.toLocaleString('pt-BR', { month: 'long', year: 'numeric' });
        const labelCap = label.charAt(0).toUpperCase() + label.slice(1);
        monthSelect.innerHTML += `<option value="${val}">${labelCap}</option>`;
    }
}

async function exportExcel() {
    const btn = document.getElementById('btn-export-excel');
    if (!btn) return;
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Gerando...';
    btn.disabled = true;

    try {
        const XLSX = await import('xlsx');
        const selectedMonth = document.getElementById('export-month-select').value;
        const [year, month] = selectedMonth.split('-').map(Number);

        // Date range of the 12 most recent months ending at selectedMonth
        const startDate = new Date(year, month - 1 - 11, 1);
        const endDate = new Date(year, month, 0); // Last day of selectedMonth

        const formatDate = (d) => {
            const y = d.getFullYear();
            const m = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${day}`;
        };

        const startStr = formatDate(startDate);
        const endStr = formatDate(endDate);

        // Fetch transactions for the date range
        const transactions = await transactionsApi.getByDateRange(startStr, endStr);

        // Create the 12 months array (sorted chronologically)
        const monthsList = [];
        for (let i = 11; i >= 0; i--) {
            const d = new Date(year, month - 1 - i, 1);
            monthsList.push({
                key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
                label: d.toLocaleString('pt-BR', { month: 'long', year: 'numeric' }).replace(/^\w/, c => c.toUpperCase()),
                entradas: 0,
                saidas: 0,
                saldo: 0,
                acumulado: 0
            });
        }

        const isIncome = (type) => type === 'REVENUE' || type === 'OTHER_REVENUE';

        // Group transactions by month
        transactions.forEach(t => {
            if (!t.date) return;
            const parts = t.date.split('-');
            if (parts.length < 2) return;
            const tYear = parseInt(parts[0], 10);
            const tMonth = parseInt(parts[1], 10);
            const key = `${tYear}-${String(tMonth).padStart(2, '0')}`;
            
            const mObj = monthsList.find(m => m.key === key);
            if (mObj) {
                const amount = parseFloat(t.amount) || 0;
                if (isIncome(t.type)) {
                    mObj.entradas += amount;
                } else {
                    mObj.saidas += amount;
                }
            }
        });

        // Compute balances
        let runningAccumulated = 0;
        monthsList.forEach(m => {
            m.saldo = m.entradas - m.saidas;
            runningAccumulated += m.saldo;
            m.acumulado = runningAccumulated;
        });

        // Map Sheet 1: Visão 12 Meses
        const sheet1Data = monthsList.map(m => ({
            'Mês': m.label,
            'Entradas (R$)': m.entradas,
            'Saídas (R$)': m.saidas,
            'Saldo Mensal (R$)': m.saldo,
            'Saldo Acumulado (R$)': m.acumulado
        }));

        // Map Sheet 2: Detalhado - [Mês]
        const selectedMonthKey = selectedMonth;
        const selectedMonthTxns = transactions.filter(t => {
            if (!t.date) return false;
            const parts = t.date.split('-');
            if (parts.length < 2) return false;
            const tYear = parseInt(parts[0], 10);
            const tMonth = parseInt(parts[1], 10);
            const key = `${tYear}-${String(tMonth).padStart(2, '0')}`;
            return key === selectedMonthKey;
        });

        // Sort selected month transactions by date asc
        selectedMonthTxns.sort((a, b) => new Date(a.date) - new Date(b.date));

        const getTypeLabel = (type) => {
            switch (type) {
                case 'REVENUE': return 'Receita';
                case 'FIXED_COST': return 'Custo Fixo';
                case 'VARIABLE_COST': return 'Custo Variável';
                case 'TAX': return 'Imposto';
                case 'DEPRECIATION': return 'Depreciação';
                case 'FINANCIAL_EXPENSE': return 'Despesa Financeira';
                case 'OTHER_REVENUE': return 'Outra Receita';
                case 'OTHER_EXPENSE': return 'Outra Despesa';
                default: return type;
            }
        };

        const formatDateBR = (dateStr) => {
            if (!dateStr) return '—';
            const parts = dateStr.split('-');
            if (parts.length < 3) return dateStr;
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        };

        const sheet2Data = selectedMonthTxns.map(t => ({
            'Data': formatDateBR(t.date),
            'Tipo': getTypeLabel(t.type),
            'Descrição': t.description,
            'Categoria': t.category || '—',
            'Valor (R$)': parseFloat(t.amount) || 0
        }));

        // Create workbook
        const wb = XLSX.utils.book_new();
        const ws1 = XLSX.utils.json_to_sheet(sheet1Data);
        const ws2 = XLSX.utils.json_to_sheet(sheet2Data);

        // Adjust column widths helper
        const fitToColumn = (data) => {
            if (!data || data.length === 0) return [];
            const keys = Object.keys(data[0]);
            return keys.map(key => {
                let maxLen = key.length;
                data.forEach(row => {
                    const val = row[key] != null ? row[key].toString() : '';
                    if (val.length > maxLen) maxLen = val.length;
                });
                return { wch: maxLen + 3 };
            });
        };

        ws1['!cols'] = fitToColumn(sheet1Data);
        ws2['!cols'] = fitToColumn(sheet2Data);

        XLSX.utils.book_append_sheet(wb, ws1, 'Visão 12 Meses');
        
        // Find the label for the selected month
        const selMonthObj = monthsList.find(m => m.key === selectedMonthKey);
        const selMonthLabel = selMonthObj ? selMonthObj.label : selectedMonthKey;
        const sheet2Name = `Detalhado - ${selMonthLabel.substring(0, 18)}`;
        XLSX.utils.book_append_sheet(wb, ws2, sheet2Name);

        XLSX.writeFile(wb, `finpred-relatorio-${selectedMonth}.xlsx`);
        showToast('Planilha Excel exportada com sucesso!', 'success');
    } catch (e) {
        console.error('Erro ao exportar Excel:', e);
        showToast('Erro ao exportar planilha.', 'error');
    } finally {
        btn.innerHTML = originalHtml;
        btn.disabled = false;
    }
}

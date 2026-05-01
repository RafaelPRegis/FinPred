/**
 * DashboardPage — Página principal com novos KPIs e Gráficos Preditivos.
 */
import { renderSidebar } from '../components/Sidebar.js';
import { formatCurrency } from '../utils/currency.js';
import { api } from '../api/client.js';

export function DashboardPage() {
    return `
        ${renderSidebar('/dashboard')}
        <main class="app-content">
            <div class="page-header" style="margin-bottom: var(--space-6);">
                <h1 style="font-size: 1.5rem; font-weight: 500; color: var(--text-secondary); display: none;">Dashboard</h1>
            </div>

            <div class="dashboard-grid stagger-children" id="kpi-grid" style="grid-template-columns: repeat(4, 1fr); margin-bottom: var(--space-6);">
                
                <div class="card kpi-card fade-in-up" style="padding: var(--space-5);">
                    <span class="kpi-label" style="text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.75rem;">Faturamento Previsto</span>
                    <div class="kpi-value money" id="kpi-revenue" style="color: var(--accent-info); font-size: 2rem; margin: var(--space-2) 0;">R$ 0,00</div>
                    <div class="kpi-change positive" style="font-size: 0.75rem;">
                        <i class="fas fa-arrow-up right"></i> <span id="kpi-revenue-sub">Mantenha dados para análise</span>
                    </div>
                </div>

                <div class="card kpi-card fade-in-up" style="padding: var(--space-5);">
                    <span class="kpi-label" style="text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.75rem;">Lucro Projetado</span>
                    <div class="kpi-value money" id="kpi-profit" style="color: var(--accent-secondary); font-size: 2rem; margin: var(--space-2) 0;">R$ 0,00</div>
                    <div class="kpi-change positive" style="font-size: 0.75rem;">
                        <i class="fas fa-arrow-up right"></i> <span id="kpi-profit-sub">0% vs mês anterior</span>
                    </div>
                </div>

                <div class="card kpi-card fade-in-up" style="padding: var(--space-5);">
                    <span class="kpi-label" style="text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.75rem;">Margem Média</span>
                    <div class="kpi-value" id="kpi-margin" style="color: var(--accent-warning); font-size: 2rem; font-family: var(--font-mono); margin: var(--space-2) 0;">0.0%</div>
                    <div class="kpi-change" style="color: var(--text-muted); font-size: 0.75rem;">
                        <span>—</span> <span id="kpi-margin-sub">Estável</span>
                    </div>
                </div>

                <div class="card kpi-card fade-in-up" style="padding: var(--space-5);">
                    <span class="kpi-label" style="text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.75rem;">Risco de Caixa</span>
                    <div class="kpi-value" id="kpi-risk" style="color: var(--accent-danger); font-size: 2rem; margin: var(--space-2) 0;">Avaliando</div>
                    <div class="kpi-change positive" style="font-size: 0.75rem;">
                        <i class="fas fa-check-circle"></i> <span id="kpi-risk-sub">Saudável</span>
                    </div>
                </div>

            </div>

            <div class="charts-row" style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-6);">
                <div class="card chart-container fade-in-up" style="padding: var(--space-6);">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-6);">
                        <h3 style="font-size: 1.1rem; font-weight: 500; color: var(--accent-info);">Faturamento: Previsto vs Realizado</h3>
                    </div>
                    <div style="position: relative; height: 280px; width: 100%;">
                        <canvas id="chart-predicted-actual"></canvas>
                    </div>
                </div>
                
                <div class="card chart-container fade-in-up" style="padding: var(--space-6);">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-6);">
                        <h3 style="font-size: 1.1rem; font-weight: 500; color: var(--accent-info);">Cenários Futuros</h3>
                    </div>
                    <div style="position: relative; height: 280px; width: 100%;">
                        <canvas id="chart-scenarios"></canvas>
                    </div>
                </div>
            </div>
        </main>
    `;
}

export async function initDashboardPage() {
    try {
        const data = await api.get('/core/dashboard');
        renderKPIs(data);
        renderCharts(data);
        
        // Atualiza o insight do dia na sidebar
        const insightEl = document.getElementById('sidebar-insight-text');
        if (insightEl && data.dailyInsight) {
            insightEl.textContent = data.dailyInsight;
            sessionStorage.setItem('dailyInsight', data.dailyInsight);
        }
    } catch (error) {
        console.warn('Dashboard sem dados ou erro de API:', error.message);
    }
}

function renderKPIs(data) {
    setText('kpi-revenue', formatCurrency(data.projectedRevenue || 0));
    setText('kpi-revenue-sub', data.revenueSubtext || '');
    
    setText('kpi-profit', formatCurrency(data.projectedProfit || 0));
    setText('kpi-profit-sub', data.profitSubtext || '');
    
    setText('kpi-margin', `${(data.averageMargin || 0).toFixed(1)}%`);
    setText('kpi-margin-sub', data.marginSubtext || '');
    
    setText('kpi-risk', data.cashRisk || 'N/A');
    setText('kpi-risk-sub', data.riskSubtext || '');
    
    // Cor do risco de caixa
    const riskEl = document.getElementById('kpi-risk');
    if (riskEl && data.cashRisk) {
        if (data.cashRisk.toLowerCase() === 'baixo') riskEl.style.color = '#ef4444'; // Seguindo a imagem
        else if (data.cashRisk.toLowerCase() === 'alto') riskEl.style.color = '#ef4444';
    }
}

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

let predictedActualChart = null;
let scenariosChart = null;

async function renderCharts(data) {
    const { Chart, registerables } = await import('chart.js');
    Chart.register(...registerables);

    Chart.defaults.color = '#94a3b8';
    Chart.defaults.font.family = "'Inter', sans-serif";

    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
            legend: { 
                position: 'top', 
                align: 'end',
                labels: { 
                    boxWidth: 8, 
                    usePointStyle: true, 
                    pointStyle: 'circle',
                    font: { size: 10, weight: 600 },
                    color: '#94a3b8',
                    padding: 20
                } 
            },
            tooltip: {
                backgroundColor: 'rgba(15, 23, 42, 0.9)',
                titleColor: '#fff',
                bodyColor: '#cbd5e1',
                borderColor: 'rgba(255,255,255,0.1)',
                borderWidth: 1,
                padding: 12
            }
        },
        scales: {
            x: { 
                grid: { display: false, drawBorder: false }
            },
            y: { 
                grid: { color: 'rgba(255,255,255,0.05)', drawBorder: false },
                ticks: {
                    callback: function(value) {
                        return value.toLocaleString('pt-BR');
                    }
                }
            }
        }
    };

    // 1. Gráfico: Previsto vs Realizado
    const paCtx = document.getElementById('chart-predicted-actual');
    if (paCtx && data.predictedVsActual) {
        if (predictedActualChart) predictedActualChart.destroy();
        
        predictedActualChart = new Chart(paCtx, {
            type: 'line',
            data: {
                labels: data.predictedVsActual.map(d => d.label),
                datasets: [
                    {
                        label: 'PREVISTO',
                        data: data.predictedVsActual.map(d => d.predicted),
                        borderColor: '#3b82f6',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        borderDash: [5, 5],
                        pointBackgroundColor: '#3b82f6',
                        pointBorderColor: '#0f172a',
                        pointRadius: 4,
                        tension: 0.4
                    },
                    {
                        label: 'REALIZADO',
                        data: data.predictedVsActual.map(d => d.actual),
                        borderColor: '#06b6d4',
                        backgroundColor: 'transparent',
                        borderWidth: 3,
                        pointBackgroundColor: '#06b6d4',
                        pointBorderColor: '#0f172a',
                        pointRadius: 4,
                        tension: 0.4
                    }
                ]
            },
            options: commonOptions
        });
    }

    // 2. Gráfico: Cenários Futuros
    const scCtx = document.getElementById('chart-scenarios');
    if (scCtx && data.futureScenarios) {
        if (scenariosChart) scenariosChart.destroy();
        
        scenariosChart = new Chart(scCtx, {
            type: 'line',
            data: {
                labels: data.futureScenarios.map(d => d.label),
                datasets: [
                    {
                        label: 'Otimista',
                        data: data.futureScenarios.map(d => d.optimistic),
                        borderColor: '#10b981',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        pointBackgroundColor: '#10b981',
                        pointRadius: 3,
                        tension: 0.4
                    },
                    {
                        label: 'Neutro',
                        data: data.futureScenarios.map(d => d.neutral),
                        borderColor: '#3b82f6',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        pointBackgroundColor: '#3b82f6',
                        pointRadius: 3,
                        tension: 0.4
                    },
                    {
                        label: 'Pessimista',
                        data: data.futureScenarios.map(d => d.pessimistic),
                        borderColor: '#ef4444',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        pointBackgroundColor: '#ef4444',
                        pointRadius: 3,
                        tension: 0.4
                    }
                ]
            },
            options: commonOptions
        });
    }
}

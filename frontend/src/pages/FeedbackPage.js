import { renderSidebar } from '../components/Sidebar.js';
import { renderMAPEBadge } from '../components/MAPEBadge.js';
import { feedbackApi } from '../api/feedback.js';
import { predictionsApi } from '../api/predictions.js';
import { showToast } from '../components/Toast.js';

/**
 * FeedbackPage — Previsto vs Realizado
 * 
 * Permite ao usuário comparar previsões passadas com valores reais,
 * submeter feedback e visualizar a precisão do modelo.
 */
export function FeedbackPage() {
    return `
        ${renderSidebar('/feedback')}
        <main class="main-content" id="feedback-page">
            <header class="page-header">
                <div class="page-header-text">
                    <h1>Feedback <span class="accent">Preditivo</span></h1>
                    <p>Compare previsões com resultados reais e melhore a precisão do modelo</p>
                </div>
            </header>

            <div class="feedback-layout">
                <!-- Coluna Esquerda: Gauge + Stats -->
                <div class="feedback-sidebar-panel">
                    <div class="card feedback-accuracy-card" id="accuracy-card">
                        <h3><i class="fas fa-bullseye"></i> Precisão do Modelo</h3>
                        <div id="mape-gauge-container" class="mape-gauge-container">
                            <div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i></div>
                        </div>
                    </div>

                    <div class="card feedback-stats-card" id="stats-card">
                        <h3><i class="fas fa-chart-pie"></i> Estatísticas</h3>
                        <div class="stats-grid" id="stats-grid">
                            <div class="stat-item">
                                <span class="stat-value" id="stat-total">—</span>
                                <span class="stat-label">Feedbacks</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value" id="stat-best">—</span>
                                <span class="stat-label">Melhor Mês</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value" id="stat-worst">—</span>
                                <span class="stat-label">Pior Mês</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value" id="stat-algo">—</span>
                                <span class="stat-label">Algoritmo</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Coluna Direita: Formulário + Tabela + Gráfico -->
                <div class="feedback-main-panel">
                    <!-- Formulário de Feedback -->
                    <div class="card feedback-form-card">
                        <h3><i class="fas fa-plus-circle"></i> Registrar Resultado Real</h3>
                        <form id="feedback-form" class="feedback-form">
                            <div class="form-row">
                                <div class="form-group">
                                    <label for="feedback-prediction">Predição</label>
                                    <select id="feedback-prediction" required>
                                        <option value="">Selecione uma predição...</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label for="feedback-month">Mês de Referência</label>
                                    <input type="month" id="feedback-month" required />
                                </div>
                                <div class="form-group">
                                    <label for="feedback-actual">Valor Realizado (R$)</label>
                                    <input type="number" id="feedback-actual" step="0.01" min="0" 
                                           placeholder="Ex: 125000.00" required />
                                </div>
                                <div class="form-group form-group-btn">
                                    <button type="submit" class="btn btn-primary" id="btn-submit-feedback">
                                        <i class="fas fa-paper-plane"></i> Enviar
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>

                    <!-- Gráfico Previsto vs Realizado -->
                    <div class="card feedback-chart-card">
                        <h3><i class="fas fa-chart-area"></i> Previsto vs Realizado</h3>
                        <div class="chart-container" id="feedback-chart-container">
                            <canvas id="feedback-chart"></canvas>
                        </div>
                    </div>

                    <!-- Tabela de Histórico -->
                    <div class="card feedback-table-card">
                        <h3><i class="fas fa-history"></i> Histórico de Feedback</h3>
                        <div class="table-responsive">
                            <table class="data-table" id="feedback-table">
                                <thead>
                                    <tr>
                                        <th>Mês</th>
                                        <th>Previsto</th>
                                        <th>Realizado</th>
                                        <th>Erro %</th>
                                        <th>MAPE Acum.</th>
                                        <th>Correção</th>
                                    </tr>
                                </thead>
                                <tbody id="feedback-tbody">
                                    <tr>
                                        <td colspan="6" class="empty-cell">
                                            <i class="fas fa-inbox"></i> Nenhum feedback registrado
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    `;
}

/**
 * Inicializa a FeedbackPage — carrega dados e configura handlers.
 */
export async function initFeedbackPage() {
    await Promise.all([
        loadAccuracy(),
        loadHistory(),
        loadPredictions()
    ]);

    // Form handler
    const form = document.getElementById('feedback-form');
    if (form) {
        form.addEventListener('submit', handleSubmitFeedback);
    }
}

/**
 * Carrega e exibe a acurácia do modelo.
 */
async function loadAccuracy() {
    const container = document.getElementById('mape-gauge-container');
    try {
        const accuracy = await feedbackApi.getAccuracy();
        if (container) {
            container.innerHTML = renderMAPEBadge(accuracy, 'large');
        }

        // Stats
        const statTotal = document.getElementById('stat-total');
        const statAlgo = document.getElementById('stat-algo');
        if (statTotal) statTotal.textContent = accuracy.totalFeedbacks || '0';
        if (statAlgo) {
            const algoNames = {
                'ARMA': 'ARMA',
                'HOLT_WINTERS': 'Holt-Winters',
                'LINEAR_REGRESSION': 'Regressão',
                'N/A': 'N/A'
            };
            statAlgo.textContent = algoNames[accuracy.bestAlgorithm] || accuracy.bestAlgorithm || 'N/A';
        }
    } catch (err) {
        console.error('Erro ao carregar acurácia:', err);
        if (container) {
            container.innerHTML = renderMAPEBadge(null);
        }
    }
}

/**
 * Carrega e exibe o histórico de feedbacks.
 */
async function loadHistory() {
    const tbody = document.getElementById('feedback-tbody');
    if (!tbody) return;

    try {
        const history = await feedbackApi.getHistory();

        if (!history || history.length === 0) {
            tbody.innerHTML = `
                <tr><td colspan="6" class="empty-cell">
                    <i class="fas fa-inbox"></i> Nenhum feedback registrado ainda
                </td></tr>
            `;
            return;
        }

        // Atualizar stats de melhor/pior mês
        let bestError = Infinity, worstError = 0, bestMonth = '', worstMonth = '';
        history.forEach(f => {
            const err = parseFloat(f.errorPercentage) || 0;
            const monthLabel = f.month ? new Date(f.month + '-01').toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' }) : '—';
            if (err < bestError) { bestError = err; bestMonth = monthLabel; }
            if (err > worstError) { worstError = err; worstMonth = monthLabel; }
        });

        const statBest = document.getElementById('stat-best');
        const statWorst = document.getElementById('stat-worst');
        if (statBest) statBest.textContent = bestMonth || '—';
        if (statWorst) statWorst.textContent = worstMonth || '—';

        tbody.innerHTML = history.map(f => {
            const error = parseFloat(f.errorPercentage) || 0;
            const errorClass = error < 10 ? 'text-success' : error < 20 ? 'text-warning' : 'text-danger';
            const monthLabel = f.month
                ? new Date(f.month + '-01').toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })
                : '—';

            return `
                <tr>
                    <td>${monthLabel}</td>
                    <td>R$ ${formatCurrency(f.predictedValue)}</td>
                    <td>R$ ${formatCurrency(f.actualValue)}</td>
                    <td class="${errorClass}">${error.toFixed(1)}%</td>
                    <td>${f.mapeScore ? parseFloat(f.mapeScore).toFixed(1) + '%' : '—'}</td>
                    <td>${f.correctionFactor ? parseFloat(f.correctionFactor).toFixed(4) : '—'}</td>
                </tr>
            `;
        }).join('');

        // Renderizar gráfico
        renderFeedbackChart(history);
    } catch (err) {
        console.error('Erro ao carregar histórico:', err);
        tbody.innerHTML = `
            <tr><td colspan="6" class="empty-cell text-danger">
                <i class="fas fa-exclamation-circle"></i> Erro ao carregar histórico
            </td></tr>
        `;
    }
}

/**
 * Carrega predições salvas para o dropdown do formulário.
 */
async function loadPredictions() {
    const select = document.getElementById('feedback-prediction');
    if (!select) return;

    try {
        const predictions = await predictionsApi.getHistory();

        if (!predictions || predictions.length === 0) {
            select.innerHTML = '<option value="">Nenhuma predição disponível</option>';
            return;
        }

        select.innerHTML = '<option value="">Selecione uma predição...</option>' +
            predictions.map(p => {
                const label = p.monthLabel || p.month;
                const value = formatCurrency(p.predictedRevenue);
                return `<option value="${p.id}">
                    ${label} — R$ ${value} (${p.algorithm || 'N/A'})
                </option>`;
            }).join('');
    } catch (err) {
        console.error('Erro ao carregar predições:', err);
        select.innerHTML = '<option value="">Erro ao carregar predições</option>';
    }
}

/**
 * Handler de submit do formulário de feedback.
 */
async function handleSubmitFeedback(e) {
    e.preventDefault();

    const predictionId = document.getElementById('feedback-prediction')?.value;
    const month = document.getElementById('feedback-month')?.value;
    const actualValue = document.getElementById('feedback-actual')?.value;

    if (!predictionId || !month || !actualValue) {
        showToast('Preencha todos os campos', 'warning');
        return;
    }

    const btn = document.getElementById('btn-submit-feedback');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
    }

    try {
        await feedbackApi.submit({
            predictionId: parseInt(predictionId),
            month: month + '-01', // Converter "2026-01" para "2026-01-01"
            actualValue: parseFloat(actualValue)
        });

        showToast('Feedback registrado com sucesso!', 'success');

        // Recarregar dados
        await Promise.all([loadAccuracy(), loadHistory()]);

        // Limpar formulário
        document.getElementById('feedback-form')?.reset();
    } catch (err) {
        console.error('Erro ao submeter feedback:', err);
        showToast('Erro ao registrar feedback. Tente novamente.', 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar';
        }
    }
}

/**
 * Renderiza gráfico de linhas Previsto vs Realizado usando Chart.js.
 */
function renderFeedbackChart(history) {
    const canvas = document.getElementById('feedback-chart');
    if (!canvas || !window.Chart) return;

    const ctx = canvas.getContext('2d');

    // Ordenar cronologicamente
    const sorted = [...history].sort((a, b) => (a.month || '').localeCompare(b.month || ''));

    const labels = sorted.map(f =>
        f.month ? new Date(f.month + '-01').toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' }) : '—'
    );
    const predicted = sorted.map(f => parseFloat(f.predictedValue) || 0);
    const actual = sorted.map(f => parseFloat(f.actualValue) || 0);

    // Destruir gráfico anterior se existir
    if (window._feedbackChart) {
        window._feedbackChart.destroy();
    }

    window._feedbackChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: 'Previsto',
                    data: predicted,
                    borderColor: '#7c4dff',
                    backgroundColor: 'rgba(124, 77, 255, 0.1)',
                    fill: true,
                    tension: 0.4,
                    borderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: '#7c4dff'
                },
                {
                    label: 'Realizado',
                    data: actual,
                    borderColor: '#00e5ff',
                    backgroundColor: 'rgba(0, 229, 255, 0.1)',
                    fill: true,
                    tension: 0.4,
                    borderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: '#00e5ff'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: { color: '#b0bec5', font: { family: "'Inter', sans-serif" } }
                },
                tooltip: {
                    backgroundColor: 'rgba(20, 20, 35, 0.95)',
                    titleColor: '#fff',
                    bodyColor: '#b0bec5',
                    borderColor: 'rgba(124, 77, 255, 0.3)',
                    borderWidth: 1,
                    callbacks: {
                        label: (ctx) => `${ctx.dataset.label}: R$ ${ctx.parsed.y.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: '#78909c' },
                    grid: { color: 'rgba(255,255,255,0.04)' }
                },
                y: {
                    ticks: {
                        color: '#78909c',
                        callback: (v) => 'R$ ' + (v / 1000).toFixed(0) + 'k'
                    },
                    grid: { color: 'rgba(255,255,255,0.04)' }
                }
            }
        }
    });
}

/**
 * Formata valor para moeda brasileira.
 */
function formatCurrency(value) {
    const num = parseFloat(value) || 0;
    return num.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

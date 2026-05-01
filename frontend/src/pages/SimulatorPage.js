import { renderSidebar } from '../components/Sidebar.js';
import { predictionsApi } from '../api/predictions.js';
import { productsApi } from '../api/products.js';
import { formatCurrency } from '../utils/currency.js';
import { showToast } from '../components/Toast.js';

let chartInstance = null;
let productsData = [];
let selectedProduct = null;

export function SimulatorPage() {
    return `
        ${renderSidebar('/simulator')}
        <main class="app-content">
            <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
                <div>
                    <h1>Simulador de Cenários</h1>
                    <p>Projete seu faturamento e calcule a aceitação populacional alterando suas margens.</p>
                </div>
                <div style="display: flex; gap: var(--space-3); align-items: center;">
                    <div class="form-group" style="margin: 0; min-width: 250px;">
                        <select id="sim-product-select" class="form-control select">
                            <option value="">-- Simulação Livre --</option>
                        </select>
                    </div>
                    <button id="btn-reset" class="btn btn-secondary" style="display: none;" title="Redefinir para Valores Originais">
                        <i class="fas fa-undo"></i>
                    </button>
                    <button id="btn-save" class="btn btn-primary" style="display: none;" title="Salvar Alterações no Produto">
                        <i class="fas fa-save"></i> Salvar
                    </button>
                </div>
            </div>

            <div class="dashboard-grid" style="grid-template-columns: 2fr 1fr; margin-bottom: var(--space-6);">
                
                <!-- PAINEL DE CONTROLE (SLIDERS) -->
                <div class="sim-slider-group fade-in-up">
                    
                    <!-- Preço de Venda -->
                    <div class="sim-slider-row">
                        <div class="sim-slider-header">
                            <div class="sim-slider-labels">
                                <h4>Preço de Venda</h4>
                                <p>Ajuste o valor final para o consumidor.</p>
                            </div>
                            <div class="sim-slider-value cyan" id="val-price-display" style="cursor: pointer;" title="Clique para digitar">R$ <span id="val-price" contenteditable="true">150</span></div>
                        </div>
                        <input type="range" id="sim-price" class="sim-range cyan" min="0.1" max="1000" step="0.1" value="150">
                    </div>

                    <!-- Custo -->
                    <div class="sim-slider-row">
                        <div class="sim-slider-header">
                            <div class="sim-slider-labels">
                                <h4>Custo de Aquisição/Produção</h4>
                                <p>Custo direto por unidade vendida.</p>
                            </div>
                            <div class="sim-slider-value pink" id="val-cost-display" style="cursor: pointer;" title="Clique para digitar">R$ <span id="val-cost" contenteditable="true">60</span></div>
                        </div>
                        <input type="range" id="sim-cost" class="sim-range pink" min="0.05" max="900" step="0.05" value="60">
                    </div>

                    <!-- Volume -->
                    <div class="sim-slider-row">
                        <div class="sim-slider-header">
                            <div class="sim-slider-labels">
                                <h4>Volume de Vendas Mensal</h4>
                                <p>Quantidade estimada de vendas ao mês.</p>
                            </div>
                            <div class="sim-slider-value yellow" id="val-volume-display" style="cursor: pointer;" title="Clique para digitar"><span id="val-volume" contenteditable="true">300</span></div>
                        </div>
                        <input type="range" id="sim-volume" class="sim-range yellow" min="1" max="5000" step="1" value="300">
                    </div>

                    <!-- Crescimento -->
                    <div class="sim-slider-row">
                        <div class="sim-slider-header">
                            <div class="sim-slider-labels">
                                <h4>Crescimento Anual Esperado</h4>
                                <p>Porcentagem de crescimento projetada para o ano.</p>
                            </div>
                            <div class="sim-slider-value purple" id="val-growth-display" style="cursor: pointer;" title="Clique para digitar"><span id="val-growth" contenteditable="true">10</span>%</div>
                        </div>
                        <input type="range" id="sim-growth" class="sim-range purple" min="-50" max="100" step="1" value="10">
                    </div>

                </div>

                <!-- PAINEL DE KPIs -->
                <div class="sim-kpis fade-in-up" style="animation-delay: 0.1s;">
                    
                    <!-- KPI: Margem -->
                    <div class="sim-kpi-box neon-green">
                        <h5>Margem Unitária Planejada</h5>
                        <div class="sim-kpi-val" id="kpi-margin-val">R$ 90 <span>0% Margem de Lucro</span></div>
                    </div>

                    <!-- KPI: Resultado Mensal -->
                    <div class="sim-kpi-box neon-teal">
                        <h5>Lucro Anual Estimado (Cenário Neutro)</h5>
                        <div class="sim-kpi-val" id="kpi-annual-profit">R$ 0</div>
                        <p>Lucro total projetado após custos.</p>
                    </div>

                    <!-- KPI: Aceitação Populacional -->
                    <div class="sim-kpi-box neon-green">
                        <h5>Aceitação Populacional</h5>
                        <div class="sim-kpi-val" id="kpi-acceptance-val">100% <span>EXCELENTE</span></div>
                        <div class="sim-progress-track">
                            <div class="sim-progress-bar" id="kpi-acceptance-bar" style="width: 100%;"></div>
                        </div>
                        <p style="margin-top: 8px;">Baseado em métricas de elasticidade-preço de Weber-Fechner.</p>
                    </div>

                </div>
            </div>

            <!-- GRÁFICO 12 MESES -->
            <div class="card fade-in-up" style="animation-delay: 0.2s;">
                <h3 style="color: #06b6d4; font-size: 1.1rem; margin-bottom: 1rem;">Projeção de Curva de Ponto de Equilíbrio</h3>
                <div class="chart-container" style="position: relative; height: 350px; width: 100%;">
                    <canvas id="simulatorChart"></canvas>
                </div>
            </div>
        </main>
    `;
}

export async function initSimulatorPage() {
    setupSliders();
    
    // Bind buttons
    const inputs = ['sim-price', 'sim-cost', 'sim-volume', 'sim-growth'];
    inputs.forEach(id => {
        document.getElementById(id).addEventListener('input', debounce(runSimulation, 300));
    });

    document.getElementById('sim-product-select').addEventListener('change', handleProductSelection);
    document.getElementById('btn-reset').addEventListener('click', resetProductValues);
    document.getElementById('btn-save').addEventListener('click', saveProductValues);

    await loadProducts();
    runSimulation();
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

async function loadProducts() {
    try {
        const response = await productsApi.getAll();
        productsData = response || [];
        
        const select = document.getElementById('sim-product-select');
        productsData.forEach(p => {
            const option = document.createElement('option');
            option.value = p.id;
            option.textContent = p.name;
            select.appendChild(option);
        });
    } catch (error) {
        console.error("Erro ao carregar produtos:", error);
    }
}

function handleProductSelection(e) {
    const productId = e.target.value;
    const btnReset = document.getElementById('btn-reset');
    const btnSave = document.getElementById('btn-save');

    if (!productId) {
        selectedProduct = null;
        btnReset.style.display = 'none';
        btnSave.style.display = 'none';
        return;
    }

    selectedProduct = productsData.find(p => p.id == productId);
    btnReset.style.display = 'block';
    btnSave.style.display = 'block';
    
    resetProductValues();
}

function resetProductValues() {
    if (!selectedProduct) return;
    
    const priceInput = document.getElementById('sim-price');
    const costInput = document.getElementById('sim-cost');
    
    priceInput.value = selectedProduct.salePrice;
    costInput.value = selectedProduct.costPrice;
    
    // Trigger input events to update labels
    priceInput.dispatchEvent(new Event('input'));
    costInput.dispatchEvent(new Event('input'));
    
    runSimulation();
}

async function saveProductValues() {
    if (!selectedProduct) return;

    const btn = document.getElementById('btn-save');
    const originalHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    btn.disabled = true;

    const newPrice = parseFloat(document.getElementById('sim-price').value);
    const newCost = parseFloat(document.getElementById('sim-cost').value);

    const updateData = {
        name: selectedProduct.name,
        category: selectedProduct.category,
        salePrice: newPrice,
        costPrice: newCost,
        active: selectedProduct.active
    };

    try {
        await productsApi.update(selectedProduct.id, updateData);
        showToast('Produto atualizado com sucesso!', 'success');
        
        // Atualiza cache local
        selectedProduct.salePrice = newPrice;
        selectedProduct.costPrice = newCost;
    } catch (error) {
        showToast('Erro ao atualizar produto.', 'error');
    } finally {
        btn.innerHTML = originalHtml;
        btn.disabled = false;
    }
}

function setupSliders() {
    const updateLabel = (id, val) => {
        const displayVal = parseFloat(val).toLocaleString('pt-BR', {minimumFractionDigits: 0, maximumFractionDigits: 2});
        const target = document.getElementById(id);
        if (target.textContent !== displayVal) {
            target.textContent = displayVal;
        }
    };

    const inputs = ['price', 'cost', 'volume', 'growth'];
    
    inputs.forEach(key => {
        const slider = document.getElementById(`sim-${key}`);
        const textSpan = document.getElementById(`val-${key}`);
        
        // Quando o slider mexe, atualiza o texto (sem formatação pesada para não bugar o editável enquanto digita)
        slider.addEventListener('input', (e) => {
            updateLabel(`val-${key}`, e.target.value);
        });

        // Quando o usuário clica e edita o texto
        textSpan.addEventListener('blur', (e) => {
            let newVal = parseFloat(e.target.textContent.replace(',', '.'));
            if (isNaN(newVal)) {
                newVal = slider.value; // restaura o antigo se for inválido
            }
            slider.value = newVal;
            updateLabel(`val-${key}`, newVal);
            slider.dispatchEvent(new Event('input')); // Dispara a simulação
        });

        // Permitir Enter para confirmar a edição
        textSpan.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                textSpan.blur();
            }
        });
    });
}

function calculateAcceptance(currentPrice) {
    let referencePrice = 150; // default base price se não houver produto

    if (selectedProduct) {
        referencePrice = selectedProduct.salePrice;
    } else {
        // Se não há produto, a referência é o custo * 2 (100% markup ideal)
        const currentCost = parseFloat(document.getElementById('sim-cost').value);
        referencePrice = currentCost * 2;
        if (referencePrice < 10) referencePrice = 10;
    }

    if (currentPrice <= referencePrice) {
        return 100;
    }

    // Lei de Weber-Fechner: Decaimento logarítmico
    // Constante K = 200. Se P/Pref = 1.2 (20% aumento), ln(1.2) = 0.18. Queda = 36%
    const ratio = currentPrice / referencePrice;
    const acceptance = 100 - (200 * Math.log(ratio));

    return Math.max(0, Math.min(100, acceptance)); // Clamp entre 0 e 100
}

function updateLocalKPIs(price, cost, acceptance) {
    // Margem Unitária
    const unitProfit = price - cost;
    const marginPercent = price > 0 ? (unitProfit / price) * 100 : 0;
    
    const marginEl = document.getElementById('kpi-margin-val');
    marginEl.innerHTML = `R$ ${unitProfit.toLocaleString('pt-BR', {minimumFractionDigits: 2})} <span>${marginPercent.toFixed(1)}% Margem</span>`;
    
    // Altera cor se negativo
    if (unitProfit < 0) {
        marginEl.parentElement.className = 'sim-kpi-box neon-green'; // reuse green but red text inside
        marginEl.style.color = '#ef4444';
        marginEl.querySelector('span').style.color = '#ef4444';
    } else {
        marginEl.style.color = '#10b981';
        marginEl.querySelector('span').style.color = '#10b981';
    }

    // Aceitação
    const accEl = document.getElementById('kpi-acceptance-val');
    const accBar = document.getElementById('kpi-acceptance-bar');
    
    let statusText = 'EXCELENTE';
    let color = '#10b981';
    if (acceptance < 50) {
        statusText = 'CRÍTICA';
        color = '#ef4444';
    } else if (acceptance < 80) {
        statusText = 'MODERADA';
        color = '#f59e0b';
    }

    accEl.innerHTML = `${acceptance.toFixed(0)}% <span style="color: ${color}">${statusText}</span>`;
    accEl.style.color = color;
    accBar.style.width = `${acceptance}%`;
    accBar.style.backgroundColor = color;
}

async function runSimulation() {
    const price = parseFloat(document.getElementById('sim-price').value);
    const cost = parseFloat(document.getElementById('sim-cost').value);
    const volume = parseInt(document.getElementById('sim-volume').value);
    const growth = parseFloat(document.getElementById('sim-growth').value);

    // 1. Atualiza KPIs Locais (instantâneo)
    const acceptance = calculateAcceptance(price);
    updateLocalKPIs(price, cost, acceptance);

    // 2. Chama backend para projeção dos 12 meses
    const params = {
        basePrice: price,
        baseCost: cost,
        baseVolume: volume,
        expectedGrowthRate: growth
    };

    try {
        const result = await predictionsApi.simulate(params);
        updateChartAndProfit(result);
    } catch (error) {
        console.error("Erro na simulação", error);
    }
}

function updateChartAndProfit(data) {
    const { pessimist, neutral, optimist } = data;

    // Atualiza Lucro Anual Estimado
    document.getElementById('kpi-annual-profit').innerHTML = formatCurrency(neutral.totalAnnualProfit) + ` <span>Cenário Neutro</span>`;

    const labels = neutral.projections.map(p => p.monthLabel);
    const pessimistData = pessimist.projections.map(p => p.profit);
    const neutralData = neutral.projections.map(p => p.profit);
    const optimistData = optimist.projections.map(p => p.profit);

    renderChart(labels, pessimistData, neutralData, optimistData);
}

function renderChart(labels, pessimist, neutral, optimist) {
    const ctx = document.getElementById('simulatorChart');
    if (!ctx) return;

    if (chartInstance) {
        chartInstance.destroy();
    }

    Chart.defaults.color = '#94a3b8';
    Chart.defaults.font.family = "'Inter', sans-serif";

    chartInstance = new Chart(ctx.getContext('2d'), {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Cenário Otimista',
                    data: optimist,
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    borderWidth: 2,
                    borderDash: [5, 5],
                    tension: 0.4,
                    pointRadius: 0
                },
                {
                    label: 'Cenário Neutro',
                    data: neutral,
                    borderColor: '#06b6d4', // Cyan
                    backgroundColor: 'rgba(6, 182, 212, 0.2)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 4,
                    pointBackgroundColor: '#06b6d4'
                },
                {
                    label: 'Cenário Pessimista',
                    data: pessimist,
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    borderWidth: 2,
                    borderDash: [5, 5],
                    tension: 0.4,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                tooltip: {
                    backgroundColor: 'rgba(15, 23, 42, 0.9)',
                    titleColor: '#f8fafc',
                    bodyColor: '#cbd5e1',
                    borderColor: 'rgba(255,255,255,0.1)',
                    borderWidth: 1,
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) label += ': ';
                            if (context.parsed.y !== null) {
                                label += new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(context.parsed.y);
                            }
                            return label;
                        }
                    }
                },
                legend: {
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        padding: 20
                    }
                }
            },
            scales: {
                x: { grid: { display: false, drawBorder: false } },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.05)', drawBorder: false },
                    ticks: {
                        callback: function(value) {
                            if (Math.abs(value) >= 1000000) {
                                return 'R$ ' + (value / 1000000).toFixed(1).replace('.0', '') + 'M';
                            } else if (Math.abs(value) >= 1000) {
                                return 'R$ ' + (value / 1000).toFixed(1).replace('.0', '') + 'k';
                            } else {
                                return 'R$ ' + value;
                            }
                        }
                    }
                }
            }
        }
    });
}

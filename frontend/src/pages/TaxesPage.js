import { renderSidebar } from '../components/Sidebar.js';
import { taxesApi } from '../api/taxes.js';
import { acquirersApi } from '../api/acquirers.js';
import { formatCurrency } from '../utils/currency.js';
import { showToast } from '../components/Toast.js';

export function TaxesPage() {
    return `
        ${renderSidebar('/taxes')}
        <main class="app-content">
            <div class="page-header">
                <h1>Calculadora Fiscal</h1>
                <p>Simule a carga tributária do seu negócio com cálculos reais (Simples Nacional e Lucro Presumido)</p>
            </div>

            <div class="card fade-in-up" style="max-width: 800px; margin: 0 auto;">
                <form id="taxes-form" class="auth-form" style="margin-bottom: var(--space-8);">
                    <div class="form-row">
                        <div class="input-group">
                            <label for="tax-revenue">Receita Mensal Simulada (R$)</label>
                            <div class="input-icon">
                                <i class="fas fa-dollar-sign"></i>
                                <input type="number" id="tax-revenue" class="input" step="0.01" min="0" placeholder="0,00" required />
                            </div>
                        </div>
                        <div class="input-group">
                            <label for="tax-regime">Regime Tributário</label>
                            <div class="input-icon">
                                <i class="fas fa-balance-scale"></i>
                                <select id="tax-regime" class="input" required>
                                    <option value="SIMPLES_NACIONAL">Simples Nacional</option>
                                    <option value="LUCRO_PRESUMIDO">Lucro Presumido</option>
                                    <option value="MEI">MEI</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    
                    <div class="input-group">
                        <label for="tax-business-type">Setor de Atuação</label>
                        <div class="input-icon">
                            <i class="fas fa-building"></i>
                            <select id="tax-business-type" class="input" required>
                                <option value="COMERCIO">Comércio / Varejo</option>
                                <option value="INDUSTRIA">Indústria</option>
                                <option value="SERVICO">Serviços</option>
                            </select>
                        </div>
                    </div>

                    <div id="simples-fields" style="background: var(--bg-tertiary); padding: var(--space-4); border-radius: 8px; margin-bottom: var(--space-4);">
                        <h4 style="margin-bottom: var(--space-3); color: var(--accent-primary);"><i class="fas fa-info-circle"></i> Dados para o Simples Nacional</h4>
                        <div class="form-row">
                            <div class="input-group">
                                <label for="tax-rbt12">Receita Acumulada 12 meses (RBT12)</label>
                                <div class="input-icon">
                                    <i class="fas fa-chart-line"></i>
                                    <input type="number" id="tax-rbt12" class="input" step="0.01" min="0" placeholder="Opcional. Projeta auto." />
                                </div>
                            </div>
                            <div class="input-group">
                                <label for="tax-payroll">Folha de Pagamento 12 meses (Fator R)</label>
                                <div class="input-icon">
                                    <i class="fas fa-users"></i>
                                    <input type="number" id="tax-payroll" class="input" step="0.01" min="0" placeholder="Necessário para Serviços" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Meio de Recebimento Section -->
                    <div id="payment-method-section" style="background: var(--bg-tertiary); padding: var(--space-4); border-radius: 8px; margin-bottom: var(--space-4);">
                        <h4 style="margin-bottom: var(--space-3); color: var(--accent-primary);"><i class="fas fa-credit-card"></i> Meio de Recebimento (Opcional)</h4>
                        <p style="font-size: 0.8rem; color: var(--text-secondary); margin-bottom: var(--space-3);">Inclua o impacto das taxas de maquininha no resultado.</p>
                        <div class="form-row">
                            <div class="input-group">
                                <label for="tax-payment-method">Método de Pagamento</label>
                                <div class="input-icon">
                                    <i class="fas fa-wallet"></i>
                                    <select id="tax-payment-method" class="input">
                                        <option value="">Não considerar</option>
                                        <option value="pix">Pix</option>
                                        <option value="debit">Débito</option>
                                        <option value="credit">Crédito à Vista</option>
                                        <option value="credit_2to6">Crédito Parcelado (2x a 6x)</option>
                                        <option value="credit_7to12">Crédito Parcelado (7x a 12x)</option>
                                    </select>
                                </div>
                            </div>
                            <div class="input-group" id="acquirer-select-group" style="display: none;">
                                <label for="tax-acquirer">Selecionar Maquininha</label>
                                <div class="input-icon">
                                    <i class="fas fa-cash-register"></i>
                                    <select id="tax-acquirer" class="input">
                                        <option value="">Carregando...</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <button type="submit" class="btn btn-primary btn-block">
                        <i class="fas fa-calculator"></i> Simular Imposto
                    </button>
                </form>

                <div id="tax-result-container" style="display: none; border-top: 1px solid var(--glass-border); padding-top: var(--space-8);">
                    <h3>Resultado da Simulação</h3>
                    <div class="dashboard-grid" style="grid-template-columns: 1fr 1fr; margin-top: var(--space-4);">
                        <div class="kpi-card" style="background: rgba(239, 68, 68, 0.05); border-color: rgba(239, 68, 68, 0.2);">
                            <span class="kpi-label">Imposto Total Estimado</span>
                            <div class="kpi-value money" id="tax-result-amount" style="color: var(--accent-danger);">R$ 0,00</div>
                        </div>
                        <div class="kpi-card">
                            <span class="kpi-label">Alíquota Efetiva (Média)</span>
                            <div class="kpi-value" id="tax-result-rate">0.00%</div>
                        </div>
                    </div>
                    
                    <div id="tax-breakdown-container" style="display: none; margin-top: var(--space-6);">
                        <h4 style="margin-bottom: var(--space-3); color: var(--text-secondary);">Detalhamento de Tributos</h4>
                        <div class="dashboard-grid" id="tax-breakdown-grid" style="grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: var(--space-3);">
                            <!-- Breakdown cards injetados dinamicamente -->
                        </div>
                    </div>

                    <!-- Painel de Impacto Financeiro Completo -->
                    <div id="tax-financial-impact" style="display: none; margin-top: var(--space-6);">
                        <h4 style="margin-bottom: var(--space-3); color: var(--accent-primary);"><i class="fas fa-chart-pie"></i> Impacto Financeiro Completo</h4>
                        <div style="background: var(--bg-tertiary); border-radius: 10px; padding: var(--space-5); border: 1px solid var(--glass-border);">
                            <div style="display: flex; flex-direction: column; gap: var(--space-3);">
                                <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom: var(--space-2); border-bottom: 1px solid var(--glass-border);">
                                    <span style="color: var(--text-secondary); font-size: 0.9rem;"><i class="fas fa-arrow-up" style="color: var(--accent-secondary); margin-right: 6px;"></i>Receita Bruta Simulada</span>
                                    <span id="fi-revenue" style="font-family: var(--font-mono); font-weight: 600; color: var(--text-primary);">R$ 0,00</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom: var(--space-2); border-bottom: 1px solid var(--glass-border);">
                                    <span style="color: var(--text-secondary); font-size: 0.9rem;"><i class="fas fa-credit-card" style="color: var(--accent-warning); margin-right: 6px;"></i>(-) Taxa da Maquininha <span id="fi-acquirer-name" style="color: var(--text-muted); font-size: 0.8rem;"></span></span>
                                    <span id="fi-acquirer-fee" style="font-family: var(--font-mono); font-weight: 600; color: var(--accent-danger);">- R$ 0,00</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom: var(--space-2); border-bottom: 1px solid var(--glass-border);">
                                    <span style="color: var(--text-secondary); font-size: 0.9rem;"><i class="fas fa-landmark" style="color: var(--accent-danger); margin-right: 6px;"></i>(-) Impostos sobre Faturamento</span>
                                    <span id="fi-taxes" style="font-family: var(--font-mono); font-weight: 600; color: var(--accent-danger);">- R$ 0,00</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; align-items: center; padding-top: var(--space-2); border-top: 2px solid var(--accent-secondary);">
                                    <span style="color: var(--accent-secondary); font-weight: 700; font-size: 1rem;"><i class="fas fa-wallet" style="margin-right: 6px;"></i>Sobra de Caixa Real</span>
                                    <span id="fi-net-result" style="font-family: var(--font-mono); font-weight: 700; font-size: 1.2rem; color: var(--accent-secondary);">R$ 0,00</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div style="margin-top: var(--space-4); padding: var(--space-4); background: var(--bg-tertiary); border-radius: 8px;">
                        <strong>Regra Aplicada:</strong> <span id="tax-result-rule"></span>
                    </div>
                </div>
            </div>
        </main>
    `;
}

let acquirersCache = [];

export async function initTaxesPage() {
    const form = document.getElementById('taxes-form');
    const regimeSelect = document.getElementById('tax-regime');
    const simplesFields = document.getElementById('simples-fields');
    const paymentMethodSelect = document.getElementById('tax-payment-method');
    const acquirerGroup = document.getElementById('acquirer-select-group');

    // Toggle Simples Nacional fields based on selected regime
    if (regimeSelect && simplesFields) {
        regimeSelect.addEventListener('change', (e) => {
            if (e.target.value === 'SIMPLES_NACIONAL') {
                simplesFields.style.display = 'block';
            } else {
                simplesFields.style.display = 'none';
            }
        });
    }

    // Toggle acquirer selector based on payment method
    if (paymentMethodSelect && acquirerGroup) {
        paymentMethodSelect.addEventListener('change', (e) => {
            if (e.target.value) {
                acquirerGroup.style.display = 'block';
                loadAcquirersDropdown();
            } else {
                acquirerGroup.style.display = 'none';
            }
        });
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await calculateTaxes();
        });
    }
}

async function loadAcquirersDropdown() {
    const select = document.getElementById('tax-acquirer');
    if (!select) return;

    // Only load once
    if (acquirersCache.length > 0) return;

    try {
        acquirersCache = await acquirersApi.getAll();
        select.innerHTML = '<option value="">Selecione uma maquininha</option>';
        acquirersCache.forEach(a => {
            select.innerHTML += `<option value="${a.id}">${a.name}</option>`;
        });
    } catch (error) {
        select.innerHTML = '<option value="">Erro ao carregar</option>';
    }
}

function getAcquirerRate(acquirerId, paymentMethod) {
    const acquirer = acquirersCache.find(a => a.id === parseInt(acquirerId));
    if (!acquirer) return { rate: 0, name: '' };

    let rate = 0;
    if (paymentMethod === 'pix') rate = acquirer.pixRate || 0;
    else if (paymentMethod === 'debit') rate = acquirer.debitRate || 0;
    else if (paymentMethod === 'credit') rate = acquirer.creditRate || 0;
    else if (paymentMethod === 'credit_2to6') rate = acquirer.creditRateInstallment2to6 ?? acquirer.creditRate ?? 0;
    else if (paymentMethod === 'credit_7to12') rate = acquirer.creditRateInstallment7to12 ?? acquirer.creditRate ?? 0;

    return { rate, name: acquirer.name };
}

async function calculateTaxes() {
    const revenue = parseFloat(document.getElementById('tax-revenue').value);
    const taxRegime = document.getElementById('tax-regime').value;
    const businessType = document.getElementById('tax-business-type').value;
    
    // Simples Nacional Fields
    const rbt12Input = document.getElementById('tax-rbt12').value;
    const rbt12 = rbt12Input ? parseFloat(rbt12Input) : null;
    
    const payrollInput = document.getElementById('tax-payroll').value;
    const payrollAmount = payrollInput ? parseFloat(payrollInput) : null;

    // Payment method fields
    const paymentMethod = document.getElementById('tax-payment-method').value;
    const acquirerId = document.getElementById('tax-acquirer').value;

    if (isNaN(revenue) || revenue < 0) {
        showToast('Informe uma receita mensal válida', 'warning');
        return;
    }

    try {
        const payload = {
            revenue,
            taxRegime,
            businessType,
            legalNature: 'OUTROS', // Temporário para retrocompatibilidade
            ...(rbt12 != null && { rbt12 }),
            ...(payrollAmount != null && { payrollAmount })
        };

        const result = await taxesApi.simulate(payload);

        document.getElementById('tax-result-container').style.display = 'block';
        document.getElementById('tax-result-amount').textContent = formatCurrency(result.estimatedTax);
        document.getElementById('tax-result-rate').textContent = `${result.effectiveRate.toFixed(2)}%`;
        document.getElementById('tax-result-rule').textContent = result.appliedRule;

        // Breakdown rendering
        const breakdownContainer = document.getElementById('tax-breakdown-container');
        const breakdownGrid = document.getElementById('tax-breakdown-grid');
        
        if (result.taxBreakdown && Object.keys(result.taxBreakdown).length > 0) {
            breakdownContainer.style.display = 'block';
            breakdownGrid.innerHTML = '';
            
            for (const [taxName, amount] of Object.entries(result.taxBreakdown)) {
                breakdownGrid.innerHTML += `
                    <div class="kpi-card" style="padding: var(--space-3);">
                        <span class="kpi-label" style="font-size: 0.75rem;">${taxName}</span>
                        <div class="kpi-value money" style="font-size: 1rem;">${formatCurrency(amount)}</div>
                    </div>
                `;
            }
        } else {
            breakdownContainer.style.display = 'none';
        }

        // Financial Impact Panel (when acquirer is selected)
        const financialImpactPanel = document.getElementById('tax-financial-impact');
        if (paymentMethod && acquirerId) {
            const { rate, name } = getAcquirerRate(acquirerId, paymentMethod);
            const acquirerFee = revenue * (rate / 100);
            const estimatedTax = parseFloat(result.estimatedTax) || 0;
            const netResult = revenue - acquirerFee - estimatedTax;

            document.getElementById('fi-revenue').textContent = formatCurrency(revenue);
            document.getElementById('fi-acquirer-name').textContent = `(${name} — ${rate.toFixed(2)}%)`;
            document.getElementById('fi-acquirer-fee').textContent = `- ${formatCurrency(acquirerFee)}`;
            document.getElementById('fi-taxes').textContent = `- ${formatCurrency(estimatedTax)}`;

            const netResultEl = document.getElementById('fi-net-result');
            netResultEl.textContent = formatCurrency(netResult);
            netResultEl.style.color = netResult >= 0 ? 'var(--accent-secondary)' : 'var(--accent-danger)';

            financialImpactPanel.style.display = 'block';
        } else {
            financialImpactPanel.style.display = 'none';
        }

        showToast('Simulação concluída', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

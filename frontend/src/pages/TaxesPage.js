import { renderSidebar } from '../components/Sidebar.js';
import { taxesApi } from '../api/taxes.js';
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

                    <div style="margin-top: var(--space-4); padding: var(--space-4); background: var(--bg-tertiary); border-radius: 8px;">
                        <strong>Regra Aplicada:</strong> <span id="tax-result-rule"></span>
                    </div>
                </div>
            </div>
        </main>
    `;
}

export async function initTaxesPage() {
    const form = document.getElementById('taxes-form');
    const regimeSelect = document.getElementById('tax-regime');
    const simplesFields = document.getElementById('simples-fields');

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

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await calculateTaxes();
        });
    }
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

        showToast('Simulação concluída', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

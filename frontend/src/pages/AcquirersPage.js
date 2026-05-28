import { renderSidebar } from '../components/Sidebar.js';
import { renderModal } from '../components/Modal.js';
import { acquirersApi } from '../api/acquirers.js';
import { formatCurrency } from '../utils/currency.js';
import { showToast } from '../components/Toast.js';

export function AcquirersPage() {
    const modalContent = `
        <form id="acquirer-form" class="auth-form">
            <div class="input-group">
                <label for="acq-name">Nome da Maquininha/Adquirente</label>
                <div class="input-icon">
                    <i class="fas fa-credit-card"></i>
                    <input type="text" id="acq-name" class="input" placeholder="Ex: Cielo, Stone, Mercado Pago" required />
                </div>
            </div>
            <div class="form-row">
                <div class="input-group">
                    <label for="acq-debit">Taxa de Débito (%)</label>
                    <div class="input-icon">
                        <i class="fas fa-percent"></i>
                        <input type="number" id="acq-debit" class="input" step="0.01" min="0" placeholder="1.99" required />
                    </div>
                </div>
                <div class="input-group">
                    <label for="acq-credit">Taxa de Crédito à Vista (%)</label>
                    <div class="input-icon">
                        <i class="fas fa-percent"></i>
                        <input type="number" id="acq-credit" class="input" step="0.01" min="0" placeholder="4.99" required />
                    </div>
                </div>
            </div>

            <!-- Parcelado Section -->
            <div style="background: var(--bg-tertiary); padding: var(--space-4); border-radius: 8px; margin-bottom: var(--space-4);">
                <h4 style="margin-bottom: var(--space-3); color: var(--accent-primary); font-size: 0.9rem;">
                    <i class="fas fa-layer-group"></i> Taxas de Crédito Parcelado
                </h4>
                <div class="form-row">
                    <div class="input-group">
                        <label for="acq-credit-2to6">Parcelado 2x a 6x (%)</label>
                        <div class="input-icon">
                            <i class="fas fa-percent"></i>
                            <input type="number" id="acq-credit-2to6" class="input" step="0.01" min="0" placeholder="5.49" />
                        </div>
                    </div>
                    <div class="input-group">
                        <label for="acq-credit-7to12">Parcelado 7x a 12x (%)</label>
                        <div class="input-icon">
                            <i class="fas fa-percent"></i>
                            <input type="number" id="acq-credit-7to12" class="input" step="0.01" min="0" placeholder="6.99" />
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-row">
                <div class="input-group">
                    <label for="acq-pix">Taxa Pix (%)</label>
                    <div class="input-icon">
                        <i class="fas fa-qrcode"></i>
                        <input type="number" id="acq-pix" class="input" step="0.01" min="0" placeholder="0.99" required />
                    </div>
                </div>
                <div class="input-group">
                    <label for="acq-fee">Mensalidade (R$)</label>
                    <div class="input-icon">
                        <i class="fas fa-dollar-sign"></i>
                        <input type="number" id="acq-fee" class="input" step="0.01" min="0" placeholder="0.00" required />
                    </div>
                </div>
            </div>
            <input type="hidden" id="acq-id" />
        </form>
    `;

    const modalFooter = `
        <button class="btn btn-secondary" onclick="closeModal('acquirer-modal')">Cancelar</button>
        <button class="btn btn-primary" id="btn-save-acquirer">
            <i class="fas fa-save"></i> Salvar
        </button>
    `;

    return `
        ${renderSidebar('/acquirers')}
        <main class="app-content">
            <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h1>Adquirentes & Maquininhas</h1>
                    <p>Compare e gerencie as taxas das suas máquinas de cartão</p>
                </div>
                <button class="btn btn-primary" onclick="openModal('acquirer-modal'); document.getElementById('acq-id').value = '';">
                    <i class="fas fa-plus"></i> Nova Adquirente
                </button>
            </div>

            <div class="charts-row" style="display: grid; grid-template-columns: 2fr 1fr; gap: var(--space-6);">
                
                <!-- Tabela de Adquirentes -->
                <div class="card" style="padding: 0; overflow: hidden; height: fit-content;">
                    <div id="acquirers-container">
                        <div class="empty-state fade-in-up">
                            <div class="loading-spinner" style="margin: var(--space-8) 0;"></div>
                            <p>Carregando adquirentes...</p>
                        </div>
                    </div>
                </div>

                <!-- Comparador -->
                <div class="card fade-in-up" style="height: fit-content; position: sticky; top: var(--space-6);">
                    <h3><i class="fas fa-balance-scale"></i> Comparador Rápido</h3>
                    <p style="color: var(--text-secondary); margin-bottom: var(--space-6); font-size: 0.9rem;">
                        Simule uma venda para ver qual máquina deixa o maior valor líquido para você.
                    </p>
                    
                    <div class="input-group">
                        <label for="comp-amount">Valor da Venda (R$)</label>
                        <div class="input-icon">
                            <i class="fas fa-dollar-sign"></i>
                            <input type="number" id="comp-amount" class="input" step="0.01" min="0" placeholder="1000,00" />
                        </div>
                    </div>
                    <div class="input-group">
                        <label for="comp-type">Método de Pagamento</label>
                        <div class="input-icon">
                            <i class="fas fa-credit-card"></i>
                            <select id="comp-type" class="input">
                                <option value="credit">Crédito à Vista</option>
                                <option value="credit_2to6">Crédito Parcelado (2x a 6x)</option>
                                <option value="credit_7to12">Crédito Parcelado (7x a 12x)</option>
                                <option value="debit">Débito</option>
                                <option value="pix">Pix</option>
                            </select>
                        </div>
                    </div>
                    
                    <button class="btn btn-primary btn-block" id="btn-compare" style="margin-bottom: var(--space-6);">
                        Comparar
                    </button>

                    <div id="compare-results">
                        <!-- Resultados renderizados aqui -->
                    </div>
                </div>
            </div>
        </main>

        ${renderModal('acquirer-modal', 'Nova Adquirente', modalContent, modalFooter)}
    `;
}

let acquirersData = [];

export async function initAcquirersPage() {
    await loadAcquirers();

    const btnSave = document.getElementById('btn-save-acquirer');
    if (btnSave) btnSave.addEventListener('click', saveAcquirer);

    const btnCompare = document.getElementById('btn-compare');
    if (btnCompare) btnCompare.addEventListener('click', runComparison);
}

async function loadAcquirers() {
    const container = document.getElementById('acquirers-container');
    if (!container) return;

    try {
        acquirersData = await acquirersApi.getAll();

        if (!acquirersData || acquirersData.length === 0) {
            container.innerHTML = `
                <div class="empty-state fade-in-up" style="padding: var(--space-12);">
                    <i class="fas fa-cash-register"></i>
                    <h3>Nenhuma adquirente cadastrada</h3>
                    <p>Cadastre suas maquininhas de cartão para usar o comparador.</p>
                    <button class="btn btn-primary" onclick="openModal('acquirer-modal')">
                        <i class="fas fa-plus"></i> Adicionar Maquininha
                    </button>
                </div>
            `;
            return;
        }

        const rows = acquirersData.map(a => {
            const inst2to6 = a.creditRateInstallment2to6 != null ? a.creditRateInstallment2to6.toFixed(2) : '—';
            const inst7to12 = a.creditRateInstallment7to12 != null ? a.creditRateInstallment7to12.toFixed(2) : '—';
            return `
            <tr>
                <td><strong>${a.name}</strong></td>
                <td><span class="badge badge-info">${a.debitRate.toFixed(2)}%</span></td>
                <td>
                    <span class="badge badge-warning">${a.creditRate.toFixed(2)}%</span>
                    <div style="font-size: 0.7rem; color: var(--text-muted); margin-top: 2px;">
                        2-6x: ${inst2to6}% · 7-12x: ${inst7to12}%
                    </div>
                </td>
                <td><span class="badge badge-success">${a.pixRate.toFixed(2)}%</span></td>
                <td class="money">${formatCurrency(a.monthlyFee)}</td>
                <td>
                    <div class="action-btns">
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="editAcquirer(${a.id})" title="Editar">
                            <i class="fas fa-pen"></i>
                        </button>
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="deleteAcquirer(${a.id}, '${a.name}')" title="Excluir" style="color: var(--accent-danger);">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `}).join('');

        container.innerHTML = `
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Adquirente</th>
                            <th>Débito</th>
                            <th>Crédito</th>
                            <th>Pix</th>
                            <th>Mensalidade</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
    } catch (error) {
        container.innerHTML = `<div class="empty-state">Erro: ${error.message}</div>`;
    }
}

async function saveAcquirer() {
    const id = document.getElementById('acq-id').value;
    const name = document.getElementById('acq-name').value.trim();
    const debitRate = parseFloat(document.getElementById('acq-debit').value);
    const creditRate = parseFloat(document.getElementById('acq-credit').value);
    const pixRate = parseFloat(document.getElementById('acq-pix').value);
    const monthlyFee = parseFloat(document.getElementById('acq-fee').value);

    // Parcelado fields (optional)
    const credit2to6Input = document.getElementById('acq-credit-2to6').value;
    const credit7to12Input = document.getElementById('acq-credit-7to12').value;
    const creditRateInstallment2to6 = credit2to6Input ? parseFloat(credit2to6Input) : null;
    const creditRateInstallment7to12 = credit7to12Input ? parseFloat(credit7to12Input) : null;

    if (!name || isNaN(debitRate) || isNaN(creditRate) || isNaN(pixRate) || isNaN(monthlyFee)) {
        showToast('Preencha todos os campos obrigatórios corretamente.', 'error');
        return;
    }

    const data = {
        name, debitRate, creditRate, pixRate, monthlyFee, active: true,
        creditRateInstallment2to6,
        creditRateInstallment7to12
    };

    try {
        if (id) {
            await acquirersApi.update(id, data);
            showToast('Adquirente atualizada', 'success');
        } else {
            await acquirersApi.create(data);
            showToast('Adquirente adicionada', 'success');
        }
        closeModal('acquirer-modal');
        await loadAcquirers();
        runComparison(); // Refresh if open
    } catch (error) {
        showToast(error.message, 'error');
    }
}

window.editAcquirer = function(id) {
    const a = acquirersData.find(x => x.id === id);
    if (!a) return;
    document.getElementById('acq-id').value = a.id;
    document.getElementById('acq-name').value = a.name;
    document.getElementById('acq-debit').value = a.debitRate;
    document.getElementById('acq-credit').value = a.creditRate;
    document.getElementById('acq-pix').value = a.pixRate;
    document.getElementById('acq-fee').value = a.monthlyFee;
    document.getElementById('acq-credit-2to6').value = a.creditRateInstallment2to6 ?? '';
    document.getElementById('acq-credit-7to12').value = a.creditRateInstallment7to12 ?? '';
    openModal('acquirer-modal');
}

window.deleteAcquirer = async function(id, name) {
    if (!confirm(`Deseja excluir a adquirente "${name}"?`)) return;
    try {
        await acquirersApi.delete(id);
        showToast('Excluída com sucesso', 'success');
        await loadAcquirers();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function runComparison() {
    const container = document.getElementById('compare-results');
    const amount = parseFloat(document.getElementById('comp-amount').value);
    const type = document.getElementById('comp-type').value;

    if (isNaN(amount) || amount <= 0 || acquirersData.length === 0) {
        container.innerHTML = '';
        return;
    }

    // Calcula líquido para cada uma
    const results = acquirersData.map(a => {
        let rate = 0;
        let rateLabel = '';
        if (type === 'credit') {
            rate = a.creditRate;
            rateLabel = 'Crédito à Vista';
        } else if (type === 'credit_2to6') {
            rate = a.creditRateInstallment2to6 ?? a.creditRate;
            rateLabel = 'Parcelado 2-6x';
        } else if (type === 'credit_7to12') {
            rate = a.creditRateInstallment7to12 ?? a.creditRate;
            rateLabel = 'Parcelado 7-12x';
        } else if (type === 'debit') {
            rate = a.debitRate;
            rateLabel = 'Débito';
        } else if (type === 'pix') {
            rate = a.pixRate;
            rateLabel = 'Pix';
        }

        const feeAmount = amount * (rate / 100);
        const netAmount = amount - feeAmount;

        return { ...a, rate, rateLabel, feeAmount, netAmount };
    });

    // Ordena do maior valor líquido para o menor
    results.sort((a, b) => b.netAmount - a.netAmount);

    let html = '<div style="display: flex; flex-direction: column; gap: var(--space-3);">';
    results.forEach((r, index) => {
        const isWinner = index === 0;
        html += `
            <div style="
                padding: var(--space-3); 
                border-radius: 8px; 
                background: ${isWinner ? 'rgba(16, 185, 129, 0.1)' : 'var(--bg-tertiary)'};
                border: 1px solid ${isWinner ? 'rgba(16, 185, 129, 0.3)' : 'transparent'};
                display: flex; justify-content: space-between; align-items: center;
            ">
                <div>
                    <strong style="color: ${isWinner ? 'var(--accent-secondary)' : 'var(--text-primary)'}">
                        ${isWinner ? '<i class="fas fa-trophy"></i> ' : ''}${r.name}
                    </strong>
                    <div style="font-size: 0.8rem; color: var(--text-secondary)">Taxa: ${r.rate.toFixed(2)}% (${r.rateLabel})</div>
                </div>
                <div style="text-align: right;">
                    <div style="font-weight: 600; color: ${isWinner ? 'var(--accent-secondary)' : 'var(--text-primary)'}">
                        ${formatCurrency(r.netAmount)}
                    </div>
                    <div style="font-size: 0.8rem; color: var(--accent-danger)">
                        -${formatCurrency(r.feeAmount)}
                    </div>
                </div>
            </div>
        `;
    });
    html += '</div>';

    container.innerHTML = html;
}

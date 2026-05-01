/**
 * CashflowPage — Fluxo de caixa com entradas/saídas e modal de transação.
 */
import { renderSidebar } from '../components/Sidebar.js';
import { renderModal } from '../components/Modal.js';
import { transactionsApi } from '../api/transactions.js';
import { formatCurrency } from '../utils/currency.js';
import { formatDateBR } from '../utils/date.js';
import { showToast } from '../components/Toast.js';

export function CashflowPage() {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];

    const modalContent = `
        <form id="transaction-form" class="auth-form">
            <div class="input-group">
                <label for="txn-type">Tipo</label>
                <select id="txn-type" class="input select" required>
                    <option value="REVENUE">💰 Receita</option>
                    <option value="FIXED_COST">📋 Custo Fixo</option>
                    <option value="VARIABLE_COST">📊 Custo Variável</option>
                </select>
            </div>
            <div class="input-group">
                <label for="txn-description">Descrição</label>
                <div class="input-icon">
                    <i class="fas fa-file-alt"></i>
                    <input type="text" id="txn-description" class="input" placeholder="Ex: Venda de produto" required />
                </div>
            </div>
            <div class="form-row">
                <div class="input-group">
                    <label for="txn-amount">Valor (R$)</label>
                    <div class="input-icon">
                        <i class="fas fa-dollar-sign"></i>
                        <input type="number" id="txn-amount" class="input" step="0.01" min="0.01" placeholder="0,00" required />
                    </div>
                </div>
                <div class="input-group">
                    <label for="txn-date">Data</label>
                    <input type="date" id="txn-date" class="input" required />
                </div>
            </div>
            <div class="input-group">
                <label for="txn-category">Categoria</label>
                <div class="input-icon">
                    <i class="fas fa-folder"></i>
                    <input type="text" id="txn-category" class="input" placeholder="Ex: Aluguel, Material..." />
                </div>
            </div>
            <input type="hidden" id="txn-id" />
        </form>
    `;

    const modalFooter = `
        <button class="btn btn-secondary" onclick="closeModal('transaction-modal')">Cancelar</button>
        <button class="btn btn-primary" id="btn-save-transaction">
            <i class="fas fa-save"></i> Salvar
        </button>
    `;

    return `
        ${renderSidebar('/cashflow')}
        <main class="app-content">
            <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h1>Fluxo de Caixa</h1>
                    <p>Acompanhe suas receitas e despesas</p>
                </div>
                <button class="btn btn-primary" id="btn-add-transaction" onclick="openModal('transaction-modal')">
                    <i class="fas fa-plus"></i> Nova Transação
                </button>
            </div>

            <!-- Resumo Rápido -->
            <div class="cashflow-summary stagger-children" id="cashflow-summary">
                <div class="card cashflow-card cashflow-revenue fade-in-up">
                    <div class="cashflow-card-icon"><i class="fas fa-arrow-up"></i></div>
                    <div>
                        <div class="cashflow-card-label">Entradas</div>
                        <div class="cashflow-card-value" id="cf-revenue">R$ 0,00</div>
                    </div>
                </div>
                <div class="card cashflow-card cashflow-cost fade-in-up">
                    <div class="cashflow-card-icon"><i class="fas fa-arrow-down"></i></div>
                    <div>
                        <div class="cashflow-card-label">Saídas</div>
                        <div class="cashflow-card-value" id="cf-costs">R$ 0,00</div>
                    </div>
                </div>
                <div class="card cashflow-card cashflow-balance fade-in-up">
                    <div class="cashflow-card-icon"><i class="fas fa-wallet"></i></div>
                    <div>
                        <div class="cashflow-card-label">Saldo</div>
                        <div class="cashflow-card-value" id="cf-balance">R$ 0,00</div>
                    </div>
                </div>
            </div>

            <!-- Filtros -->
            <div class="card" style="margin-top: var(--space-6); padding: var(--space-4) var(--space-6);">
                <div class="cashflow-filters">
                    <div class="filter-group">
                        <label for="cf-filter-start">De</label>
                        <input type="date" id="cf-filter-start" class="input" value="${firstDay}" />
                    </div>
                    <div class="filter-group">
                        <label for="cf-filter-end">Até</label>
                        <input type="date" id="cf-filter-end" class="input" value="${lastDay}" />
                    </div>
                    <div class="filter-group">
                        <label for="cf-filter-type">Tipo</label>
                        <select id="cf-filter-type" class="input select">
                            <option value="">Todos</option>
                            <option value="REVENUE">Receita</option>
                            <option value="FIXED_COST">Custo Fixo</option>
                            <option value="VARIABLE_COST">Custo Variável</option>
                        </select>
                    </div>
                    <button class="btn btn-secondary" id="btn-filter-cashflow" style="align-self: flex-end;">
                        <i class="fas fa-filter"></i> Filtrar
                    </button>
                </div>
            </div>

            <!-- Tabela de Transações -->
            <div class="card" style="margin-top: var(--space-6); padding: 0; overflow: hidden;">
                <div id="transactions-table-container">
                    <div class="empty-state fade-in-up" style="padding: var(--space-8);">
                        <div class="loading-spinner"></div>
                        <p>Carregando transações...</p>
                    </div>
                </div>
            </div>
        </main>

        ${renderModal('transaction-modal', 'Nova Transação', modalContent, modalFooter)}
    `;
}

export async function initCashflowPage() {
    // Default date no form
    const dateInput = document.getElementById('txn-date');
    if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];

    // Load data
    await loadTransactions();

    // Save handler
    const btnSave = document.getElementById('btn-save-transaction');
    if (btnSave) btnSave.addEventListener('click', saveTransaction);

    // Filter handler
    const btnFilter = document.getElementById('btn-filter-cashflow');
    if (btnFilter) btnFilter.addEventListener('click', loadTransactions);
}

async function loadTransactions() {
    const container = document.getElementById('transactions-table-container');
    if (!container) return;

    const start = document.getElementById('cf-filter-start')?.value;
    const end = document.getElementById('cf-filter-end')?.value;
    const typeFilter = document.getElementById('cf-filter-type')?.value;

    try {
        let transactions;
        if (start && end) {
            transactions = await transactionsApi.getByDateRange(start, end);
        } else {
            transactions = await transactionsApi.getAll();
        }

        // Filtro por tipo no frontend
        if (typeFilter) {
            transactions = transactions.filter(t => t.type === typeFilter);
        }

        // Sort by date desc
        transactions.sort((a, b) => new Date(b.date) - new Date(a.date));

        // Atualizar resumo
        updateSummary(transactions);

        if (!transactions || transactions.length === 0) {
            container.innerHTML = `
                <div class="empty-state fade-in-up" style="padding: var(--space-12);">
                    <i class="fas fa-receipt"></i>
                    <h3>Nenhuma transação encontrada</h3>
                    <p>Registre suas receitas e despesas para acompanhar o fluxo de caixa.</p>
                    <button class="btn btn-primary" onclick="openModal('transaction-modal')">
                        <i class="fas fa-plus"></i> Nova Transação
                    </button>
                </div>
            `;
            return;
        }

        const rows = transactions.map(t => `
            <tr>
                <td>${formatDateBR(t.date)}</td>
                <td>
                    <span class="badge ${getTypeBadge(t.type)}">${getTypeLabel(t.type)}</span>
                </td>
                <td>${t.description}</td>
                <td>${t.category || '—'}</td>
                <td class="money ${t.type === 'REVENUE' ? 'text-success' : 'text-danger'}">
                    ${t.type === 'REVENUE' ? '+' : '-'} ${formatCurrency(t.amount)}
                </td>
                <td>
                    <div class="action-btns">
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="editTransaction(${t.id})" title="Editar">
                            <i class="fas fa-pen"></i>
                        </button>
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="deleteTransaction(${t.id})" title="Excluir" style="color: var(--accent-danger);">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Data</th>
                            <th>Tipo</th>
                            <th>Descrição</th>
                            <th>Categoria</th>
                            <th>Valor</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
            <div class="table-footer">
                <span>${transactions.length} transaç${transactions.length > 1 ? 'ões' : 'ão'}</span>
            </div>
        `;
    } catch (error) {
        container.innerHTML = `
            <div class="empty-state fade-in-up" style="padding: var(--space-12);">
                <i class="fas fa-exclamation-triangle" style="color: var(--accent-warning);"></i>
                <h3>Erro ao carregar transações</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

function updateSummary(transactions) {
    const revenue = transactions
        .filter(t => t.type === 'REVENUE')
        .reduce((sum, t) => sum + parseFloat(t.amount), 0);
    const costs = transactions
        .filter(t => t.type !== 'REVENUE')
        .reduce((sum, t) => sum + parseFloat(t.amount), 0);
    const balance = revenue - costs;

    setText('cf-revenue', formatCurrency(revenue));
    setText('cf-costs', formatCurrency(costs));

    const balanceEl = document.getElementById('cf-balance');
    if (balanceEl) {
        balanceEl.textContent = formatCurrency(balance);
        balanceEl.style.color = balance >= 0 ? 'var(--accent-secondary)' : 'var(--accent-danger)';
    }
}

async function saveTransaction() {
    const id = document.getElementById('txn-id').value;
    const type = document.getElementById('txn-type').value;
    const description = document.getElementById('txn-description').value.trim();
    const amount = parseFloat(document.getElementById('txn-amount').value);
    const date = document.getElementById('txn-date').value;
    const category = document.getElementById('txn-category').value.trim();

    if (!description || isNaN(amount) || !date) {
        showToast('Preencha todos os campos obrigatórios', 'error');
        return;
    }

    const data = { type, description, amount, date, category: category || null };

    try {
        if (id) {
            await transactionsApi.update(id, data);
            showToast('Transação atualizada', 'success');
        } else {
            await transactionsApi.create(data);
            showToast('Transação registrada', 'success');
        }
        closeModal('transaction-modal');
        resetTransactionForm();
        await loadTransactions();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function resetTransactionForm() {
    document.getElementById('txn-id').value = '';
    document.getElementById('txn-type').value = 'REVENUE';
    document.getElementById('txn-description').value = '';
    document.getElementById('txn-amount').value = '';
    document.getElementById('txn-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('txn-category').value = '';
}

function getTypeBadge(type) {
    switch (type) {
        case 'REVENUE': return 'badge-success';
        case 'FIXED_COST': return 'badge-danger';
        case 'VARIABLE_COST': return 'badge-warning';
        default: return 'badge-info';
    }
}

function getTypeLabel(type) {
    switch (type) {
        case 'REVENUE': return 'Receita';
        case 'FIXED_COST': return 'Custo Fixo';
        case 'VARIABLE_COST': return 'Custo Variável';
        default: return type;
    }
}

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

// === Global handlers ===

window.editTransaction = async function(id) {
    try {
        const transactions = await transactionsApi.getAll();
        const t = transactions.find(tx => tx.id === id);
        if (!t) throw new Error('Transação não encontrada');

        document.getElementById('txn-id').value = t.id;
        document.getElementById('txn-type').value = t.type;
        document.getElementById('txn-description').value = t.description;
        document.getElementById('txn-amount').value = t.amount;
        document.getElementById('txn-date').value = t.date;
        document.getElementById('txn-category').value = t.category || '';

        const modalTitle = document.querySelector('#transaction-modal .modal-header h3');
        if (modalTitle) modalTitle.textContent = 'Editar Transação';

        openModal('transaction-modal');
    } catch (error) {
        showToast('Erro ao carregar transação', 'error');
    }
};

window.deleteTransaction = async function(id) {
    if (!confirm('Excluir esta transação?')) return;
    try {
        await transactionsApi.delete(id);
        showToast('Transação excluída', 'success');
        await loadTransactions();
    } catch (error) {
        showToast(error.message, 'error');
    }
};

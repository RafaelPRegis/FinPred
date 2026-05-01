/**
 * ProductsPage — CRUD completo de produtos com tabela e modal.
 */
import { renderSidebar } from '../components/Sidebar.js';
import { renderModal } from '../components/Modal.js';
import { productsApi } from '../api/products.js';
import { formatCurrency } from '../utils/currency.js';
import { showToast } from '../components/Toast.js';

export function ProductsPage() {
    const modalContent = `
        <form id="product-form" class="auth-form">
            <div class="input-group">
                <label for="prod-name">Nome do produto</label>
                <div class="input-icon">
                    <i class="fas fa-tag"></i>
                    <input type="text" id="prod-name" class="input" placeholder="Ex: Camiseta Premium" required />
                </div>
            </div>
            <div class="form-row">
                <div class="input-group">
                    <label for="prod-sale-price">Preço de venda (R$)</label>
                    <div class="input-icon">
                        <i class="fas fa-dollar-sign"></i>
                        <input type="number" id="prod-sale-price" class="input" step="0.01" min="0" placeholder="0,00" required />
                    </div>
                </div>
                <div class="input-group">
                    <label for="prod-cost-price">Preço de custo (R$)</label>
                    <div class="input-icon">
                        <i class="fas fa-coins"></i>
                        <input type="number" id="prod-cost-price" class="input" step="0.01" min="0" placeholder="0,00" required />
                    </div>
                </div>
            </div>
            <div class="form-row">
                <div class="input-group">
                    <label for="prod-category">Categoria</label>
                    <div class="input-icon">
                        <i class="fas fa-folder"></i>
                        <input type="text" id="prod-category" class="input" placeholder="Ex: Vestuário, Eletrônicos..." />
                    </div>
                </div>
                <div class="input-group">
                    <label for="prod-volume">Volume Estimado</label>
                    <div class="input-icon">
                        <i class="fas fa-cubes"></i>
                        <input type="number" id="prod-volume" class="input" min="0" placeholder="0" />
                    </div>
                </div>
            </div>
            <input type="hidden" id="prod-id" />
        </form>
    `;

    const modalFooter = `
        <button class="btn btn-secondary" onclick="closeModal('product-modal')">Cancelar</button>
        <button class="btn btn-primary" id="btn-save-product">
            <i class="fas fa-save"></i> Salvar
        </button>
    `;

    return `
        ${renderSidebar('/products')}
        <main class="app-content">
            <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h1>Produtos</h1>
                    <p>Gerencie os produtos do seu comércio</p>
                </div>
                <button class="btn btn-primary" id="btn-add-product" onclick="openModal('product-modal')">
                    <i class="fas fa-plus"></i> Novo Produto
                </button>
            </div>

            <div class="card" style="padding: 0; overflow: hidden;">
                <div id="products-table-container">
                    <div class="empty-state fade-in-up">
                        <div class="loading-spinner" style="margin: var(--space-8) 0;"></div>
                        <p>Carregando produtos...</p>
                    </div>
                </div>
            </div>
        </main>

        ${renderModal('product-modal', 'Novo Produto', modalContent, modalFooter)}
    `;
}

/**
 * Inicializa a página de produtos — carrega dados e configura eventos.
 */
export async function initProductsPage() {
    await loadProducts();

    // Save product
    const btnSave = document.getElementById('btn-save-product');
    if (btnSave) {
        btnSave.addEventListener('click', saveProduct);
    }
}

async function loadProducts() {
    const container = document.getElementById('products-table-container');
    if (!container) return;

    try {
        const products = await productsApi.getAll();

        if (!products || products.length === 0) {
            container.innerHTML = `
                <div class="empty-state fade-in-up" style="padding: var(--space-12);">
                    <i class="fas fa-box-open"></i>
                    <h3>Nenhum produto cadastrado</h3>
                    <p>Cadastre seus produtos para calcular margens e gerar previsões.</p>
                    <button class="btn btn-primary" onclick="openModal('product-modal')">
                        <i class="fas fa-plus"></i> Adicionar Produto
                    </button>
                </div>
            `;
            return;
        }

        const rows = products.map(p => `
            <tr>
                <td>
                    <div class="product-name-cell">
                        <span class="product-name">${p.name}</span>
                        ${p.category ? `<span class="badge badge-info">${p.category}</span>` : ''}
                    </div>
                </td>
                <td class="money">${formatCurrency(p.salePrice)}</td>
                <td class="money">${formatCurrency(p.costPrice)}</td>
                <td>
                    <span class="badge ${getMarginBadge(p.margin)}">${p.margin != null ? p.margin.toFixed(1) + '%' : '—'}</span>
                </td>
                <td>
                    <span class="badge ${p.active ? 'badge-success' : 'badge-danger'}">${p.active ? 'Ativo' : 'Inativo'}</span>
                </td>
                <td>
                    <div class="action-btns">
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="editProduct(${p.id})" title="Editar">
                            <i class="fas fa-pen"></i>
                        </button>
                        <button class="btn btn-ghost btn-sm btn-icon" onclick="deleteProduct(${p.id}, '${p.name}')" title="Excluir" style="color: var(--accent-danger);">
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
                            <th>Produto</th>
                            <th>Preço de Venda</th>
                            <th>Preço de Custo</th>
                            <th>Margem</th>
                            <th>Status</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
            <div class="table-footer">
                <span>${products.length} produto${products.length > 1 ? 's' : ''} cadastrado${products.length > 1 ? 's' : ''}</span>
            </div>
        `;
    } catch (error) {
        container.innerHTML = `
            <div class="empty-state fade-in-up" style="padding: var(--space-12);">
                <i class="fas fa-exclamation-triangle" style="color: var(--accent-warning);"></i>
                <h3>Erro ao carregar produtos</h3>
                <p>${error.message}</p>
                <button class="btn btn-secondary" onclick="location.reload()">
                    <i class="fas fa-redo"></i> Tentar novamente
                </button>
            </div>
        `;
    }
}

async function saveProduct() {
    const id = document.getElementById('prod-id').value;
    const name = document.getElementById('prod-name').value.trim();
    const salePrice = parseFloat(document.getElementById('prod-sale-price').value);
    const costPrice = parseFloat(document.getElementById('prod-cost-price').value);
    const category = document.getElementById('prod-category').value.trim();
    const volumeVal = document.getElementById('prod-volume').value;
    const estimatedVolume = volumeVal ? parseInt(volumeVal) : null;

    if (!name || isNaN(salePrice) || isNaN(costPrice)) {
        showToast('Preencha todos os campos obrigatórios', 'error');
        return;
    }

    const data = { name, salePrice, costPrice, category: category || null, estimatedVolume, active: true };

    try {
        if (id) {
            await productsApi.update(id, data);
            showToast('Produto atualizado com sucesso', 'success');
        } else {
            await productsApi.create(data);
            showToast('Produto criado com sucesso', 'success');
        }
        closeModal('product-modal');
        resetProductForm();
        await loadProducts();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function resetProductForm() {
    document.getElementById('prod-id').value = '';
    document.getElementById('prod-name').value = '';
    document.getElementById('prod-sale-price').value = '';
    document.getElementById('prod-cost-price').value = '';
    document.getElementById('prod-category').value = '';
    document.getElementById('prod-volume').value = '';

    // Resetar título do modal
    const modalTitle = document.querySelector('#product-modal .modal-header h3');
    if (modalTitle) modalTitle.textContent = 'Novo Produto';
}

function getMarginBadge(margin) {
    if (margin == null) return 'badge-info';
    if (margin >= 30) return 'badge-success';
    if (margin >= 15) return 'badge-warning';
    return 'badge-danger';
}

// === Global handlers ===

window.editProduct = async function(id) {
    try {
        const product = await productsApi.getById(id);
        document.getElementById('prod-id').value = product.id;
        document.getElementById('prod-name').value = product.name;
        document.getElementById('prod-sale-price').value = product.salePrice;
        document.getElementById('prod-cost-price').value = product.costPrice;
        document.getElementById('prod-category').value = product.category || '';
        document.getElementById('prod-volume').value = product.estimatedVolume || '';

        const modalTitle = document.querySelector('#product-modal .modal-header h3');
        if (modalTitle) modalTitle.textContent = 'Editar Produto';

        openModal('product-modal');
    } catch (error) {
        showToast('Erro ao carregar produto', 'error');
    }
};

window.deleteProduct = async function(id, name) {
    if (!confirm(`Tem certeza que deseja excluir "${name}"?`)) return;
    try {
        await productsApi.delete(id);
        showToast('Produto excluído com sucesso', 'success');
        await loadProducts();
    } catch (error) {
        showToast(error.message, 'error');
    }
};

/**
 * Componente DataTable — Tabela dinâmica com ordenação.
 */

/**
 * @param {string[]} columns - Nomes das colunas
 * @param {object[]} rows - Dados das linhas
 * @param {string} emptyMessage - Mensagem quando não há dados
 */
export function renderDataTable(columns, rows, emptyMessage = 'Nenhum dado encontrado.') {
    if (!rows || rows.length === 0) {
        return `
            <div class="empty-state">
                <i class="fas fa-inbox"></i>
                <h3>${emptyMessage}</h3>
            </div>
        `;
    }

    const headerCells = columns.map(col => `<th>${col}</th>`).join('');
    const bodyRows = rows.map(row => {
        const cells = Object.values(row).map(val => `<td>${val ?? ''}</td>`).join('');
        return `<tr>${cells}</tr>`;
    }).join('');

    return `
        <div class="table-wrapper">
            <table class="data-table">
                <thead><tr>${headerCells}</tr></thead>
                <tbody>${bodyRows}</tbody>
            </table>
        </div>
    `;
}

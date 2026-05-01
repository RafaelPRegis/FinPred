/**
 * Utilitários de formatação de moeda brasileira (BRL).
 */

/**
 * Formata um valor numérico como moeda brasileira.
 * @param {number} value - Valor a formatar
 * @returns {string} Valor formatado (ex: "R$ 1.234,56")
 */
export function formatCurrency(value) {
    if (value == null || isNaN(value)) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL',
    }).format(value);
}

/**
 * Formata um valor como porcentagem.
 * @param {number} value - Valor (ex: 15.5 = 15,5%)
 * @returns {string} Valor formatado
 */
export function formatPercent(value) {
    if (value == null || isNaN(value)) return '0,0%';
    return new Intl.NumberFormat('pt-BR', {
        style: 'percent',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
    }).format(value / 100);
}

/**
 * Formata número com separadores de milhar.
 * @param {number} value
 * @returns {string}
 */
export function formatNumber(value) {
    if (value == null || isNaN(value)) return '0';
    return new Intl.NumberFormat('pt-BR').format(value);
}

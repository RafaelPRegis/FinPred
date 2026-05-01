/**
 * Utilitários de formatação de datas para pt-BR.
 */

/**
 * Formata uma data ISO para formato brasileiro.
 * @param {string|Date} date
 * @returns {string} Ex: "30/04/2026"
 */
export function formatDate(date) {
    if (!date) return '';
    const d = new Date(date + 'T00:00:00');
    return d.toLocaleDateString('pt-BR');
}

/** Alias para formatDate — usado no Cashflow */
export const formatDateBR = formatDate;

/**
 * Formata data e hora.
 * @param {string|Date} date
 * @returns {string} Ex: "30/04/2026 15:30"
 */
export function formatDateTime(date) {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
    });
}

/**
 * Retorna o nome do mês.
 * @param {number} month - Mês (1-12)
 * @returns {string}
 */
export function getMonthName(month) {
    const months = [
        'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
        'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    return months[month - 1] || '';
}

/**
 * Retorna o nome abreviado do mês.
 * @param {number} month - Mês (1-12)
 * @returns {string}
 */
export function getMonthShort(month) {
    return getMonthName(month).substring(0, 3);
}

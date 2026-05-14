/**
 * API client para o módulo de Relatórios (Fase 6).
 */
import { api } from './client.js';

export const reportsApi = {
    /**
     * Gera DRE (Demonstração do Resultado do Exercício).
     * @param {string} start - Data início (ISO: 2026-01-01)
     * @param {string} end - Data fim (ISO: 2026-06-30)
     */
    getDre: async (start, end) => {
        let url = '/core/reports/dre';
        const params = [];
        if (start) params.push(`start=${start}`);
        if (end) params.push(`end=${end}`);
        if (params.length > 0) url += '?' + params.join('&');
        return await api.get(url);
    },

    /**
     * Gera projeção de Fluxo de Caixa (histórico + projetado).
     */
    getCashFlow: async () => {
        return await api.get('/core/reports/cashflow');
    }
};

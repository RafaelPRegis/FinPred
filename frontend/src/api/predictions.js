import { api } from './client.js';

export const predictionsApi = {
    /**
     * Simulação paramétrica (sliders do SimulatorPage).
     * @param {Object} params { basePrice, baseCost, baseVolume, expectedGrowthRate }
     */
    simulate: async (params) => {
        return await api.post('/predict/simulate', params);
    },

    /**
     * Previsão ML baseada no histórico real de transações.
     * @param {Object} params { productId?, horizon?, forceRefresh? }
     */
    forecast: async (params = {}) => {
        return await api.post('/predict/forecast', params);
    },

    /**
     * Acurácia do modelo (MAPE, classificação, algoritmo).
     */
    getAccuracy: async () => {
        return await api.get('/predict/accuracy');
    },

    /**
     * Histórico de predições salvas (cenário neutro).
     */
    getHistory: async () => {
        return await api.get('/predict/history');
    }
};

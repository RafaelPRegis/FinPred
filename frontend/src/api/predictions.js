import { api } from './client.js';

export const predictionsApi = {
    /**
     * Envia parâmetros de simulação para o Prediction Service
     * @param {Object} params { basePrice, baseCost, baseVolume, expectedGrowthRate }
     */
    simulate: async (params) => {
        return await api.post('/predict/simulate', params);
    }
};

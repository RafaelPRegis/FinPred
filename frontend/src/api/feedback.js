import { api } from './client.js';

/**
 * API client para o sistema de feedback (Previsto vs Realizado).
 */
export const feedbackApi = {
    /**
     * Submeter feedback — valor real observado vs previsão.
     * @param {Object} data { predictionId, month (ISO), actualValue }
     */
    submit: async (data) => {
        return await api.post('/predict/feedback', data);
    },

    /**
     * Buscar histórico de feedbacks do usuário.
     */
    getHistory: async () => {
        return await api.get('/predict/feedback/history');
    },

    /**
     * Buscar acurácia do modelo (MAPE + classificação).
     */
    getAccuracy: async () => {
        return await api.get('/predict/accuracy');
    }
};

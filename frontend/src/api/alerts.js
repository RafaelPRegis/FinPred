import { api } from './client.js';

/**
 * API client para alertas inteligentes do Core Service.
 */
export const alertsApi = {
    /**
     * Buscar alertas inteligentes do usuário.
     * Retorna lista ordenada por severidade (danger > warning > info > success).
     */
    getAlerts: async () => {
        return await api.get('/core/alerts');
    }
};

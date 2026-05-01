import { api } from './client.js';

export const taxesApi = {
    simulate: async (data) => {
        return await api.post('/core/taxes/simulate', data);
    }
};

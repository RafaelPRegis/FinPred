import { api } from './client.js';

export const acquirersApi = {
    getAll: async () => {
        return await api.get('/core/acquirers');
    },

    create: async (data) => {
        return await api.post('/core/acquirers', data);
    },

    update: async (id, data) => {
        return await api.put(`/core/acquirers/${id}`, data);
    },

    delete: async (id) => {
        return await api.delete(`/core/acquirers/${id}`);
    }
};

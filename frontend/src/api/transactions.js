/**
 * API de Transações.
 */
import { api } from './client.js';

export const transactionsApi = {
    getAll: () => api.get('/core/transactions'),
    getByDateRange: (start, end) => api.get(`/core/transactions?start=${start}&end=${end}`),
    create: (data) => api.post('/core/transactions', data),
    update: (id, data) => api.put(`/core/transactions/${id}`, data),
    delete: (id) => api.delete(`/core/transactions/${id}`),
};

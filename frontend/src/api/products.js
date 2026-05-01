/**
 * API de Produtos.
 */
import { api } from './client.js';

export const productsApi = {
    getAll: () => api.get('/core/products'),
    getById: (id) => api.get(`/core/products/${id}`),
    create: (data) => api.post('/core/products', data),
    update: (id, data) => api.put(`/core/products/${id}`, data),
    delete: (id) => api.delete(`/core/products/${id}`),
};

/**
 * API Client — Fetch wrapper com JWT automático.
 * Centraliza todas as chamadas HTTP para o backend via API Gateway.
 */

const API_BASE = '/api';

/** Token JWT armazenado em memória (mais seguro que localStorage) */
let authToken = null;
let currentUser = null;

export function setToken(token) {
    authToken = token;
}

export function getToken() {
    return authToken;
}

export function clearToken() {
    authToken = null;
    currentUser = null;
}

export function setUser(user) {
    currentUser = user;
}

export function getUser() {
    return currentUser;
}

/**
 * Verifica se o usuário está autenticado.
 */
export function isAuthenticated() {
    return authToken !== null;
}

/**
 * Executa uma requisição HTTP com headers padrão e JWT automático.
 * @param {string} endpoint - Path relativo (ex: '/auth/login')
 * @param {object} options - Opções do fetch
 * @returns {Promise<any>} Resposta JSON
 */
async function request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // Adiciona JWT se autenticado
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers,
        });

        // Token expirado — redirecionar para login
        if (response.status === 401) {
            clearToken();
            window.location.hash = '/';
            throw new Error('Sessão expirada. Faça login novamente.');
        }

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.error || error.message || `Erro ${response.status}`);
        }

        // Retorna null para 204 No Content
        if (response.status === 204) return null;

        return await response.json();
    } catch (error) {
        if (error.name === 'TypeError') {
            throw new Error('Servidor indisponível. Verifique se os serviços estão rodando.');
        }
        throw error;
    }
}

/**
 * HTTP Methods
 */
export const api = {
    get: (endpoint) => request(endpoint, { method: 'GET' }),
    post: (endpoint, data) => request(endpoint, { method: 'POST', body: JSON.stringify(data) }),
    put: (endpoint, data) => request(endpoint, { method: 'PUT', body: JSON.stringify(data) }),
    patch: (endpoint, data) => request(endpoint, { method: 'PATCH', body: JSON.stringify(data) }),
    delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};

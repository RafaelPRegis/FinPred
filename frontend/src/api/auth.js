/**
 * API de Autenticação.
 */
import { api } from './client.js';

export const authApi = {
    /** Etapa 1 do registro: email, nome, empresa, senha */
    registerStepOne: (data) => api.post('/auth/register', data),

    /** Etapa 2 do registro: businessType, legalNature */
    registerStepTwo: (data) => api.put('/auth/register/complete', data),

    /** Login com email e senha */
    login: (credentials) => api.post('/auth/login', credentials),

    /** Login com Google OAuth2 */
    googleLogin: (tokenData) => api.post('/auth/google', tokenData),

    /** Perfil do usuário autenticado */
    getProfile: () => api.get('/auth/profile'),

    /** Health check */
    health: () => api.get('/auth/health'),
};

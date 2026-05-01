import { api } from './client.js';

export const importApi = {
    upload: async (file) => {
        const token = localStorage.getItem('finpred_token');
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${api.baseURL}/core/import/upload`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro na requisição de upload');
        }

        return await response.json();
    },

    downloadTemplate: async () => {
        const token = localStorage.getItem('finpred_token');
        const response = await fetch(`${api.baseURL}/core/import/template/csv`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Erro ao baixar template');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'finpred_template_importacao.csv';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }
};

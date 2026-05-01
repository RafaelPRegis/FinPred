/**
 * Toast — Notificações temporárias no canto superior direito.
 */

let toastContainer = null;

function ensureContainer() {
    if (!toastContainer || !document.body.contains(toastContainer)) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        toastContainer.id = 'toast-container';
        document.body.appendChild(toastContainer);
    }
    return toastContainer;
}

/**
 * Exibe uma notificação toast.
 * @param {string} message - Mensagem
 * @param {'success'|'error'|'warning'} type - Tipo
 * @param {number} duration - Duração em ms (default: 3500)
 */
export function showToast(message, type = 'success', duration = 3500) {
    const container = ensureContainer();

    const icons = {
        success: 'fas fa-check-circle',
        error: 'fas fa-exclamation-circle',
        warning: 'fas fa-exclamation-triangle',
    };

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <i class="${icons[type] || icons.success}"></i>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    // Auto-remove
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 300ms ease';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// Expor globalmente
window.showToast = showToast;

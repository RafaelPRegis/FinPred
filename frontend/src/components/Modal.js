/**
 * Componente Modal — Modal reutilizável com glassmorphism.
 */

export function renderModal(id, title, content, footer = '') {
    return `
        <div class="modal-backdrop" id="${id}-backdrop" onclick="closeModal('${id}')">
            <div class="modal card" id="${id}" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>${title}</h3>
                    <button class="btn btn-ghost btn-icon" onclick="closeModal('${id}')">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">
                    ${content}
                </div>
                ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
            </div>
        </div>
    `;
}

// Funções globais para abrir/fechar modais
window.openModal = function(id) {
    const backdrop = document.getElementById(`${id}-backdrop`);
    if (backdrop) {
        backdrop.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
};

window.closeModal = function(id) {
    const backdrop = document.getElementById(`${id}-backdrop`);
    if (backdrop) {
        backdrop.classList.remove('active');
        document.body.style.overflow = '';
    }
};

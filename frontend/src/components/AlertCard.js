/**
 * Componente AlertCard — Cards de alerta inteligentes.
 */

/**
 * @param {'success'|'warning'|'danger'|'info'} type
 * @param {string} title
 * @param {string} message
 * @param {string} icon
 */
export function renderAlertCard(type, title, message, icon = '') {
    const icons = {
        success: 'fas fa-check-circle',
        warning: 'fas fa-exclamation-triangle',
        danger: 'fas fa-times-circle',
        info: 'fas fa-info-circle',
    };

    const iconClass = icon || icons[type] || icons.info;

    return `
        <div class="card alert-card alert-${type}">
            <div class="alert-icon">
                <i class="${iconClass}"></i>
            </div>
            <div class="alert-content">
                <h4>${title}</h4>
                <p>${message}</p>
            </div>
        </div>
    `;
}

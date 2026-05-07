/**
 * Componente AlertCard — Cards de alerta inteligentes.
 * Suporta métricas, animação de entrada e botão de dismiss.
 */

/**
 * Renderiza um card de alerta inteligente.
 * 
 * @param {'success'|'warning'|'danger'|'info'} type
 * @param {string} title
 * @param {string} message
 * @param {string} icon — Classe Font Awesome (ex: 'fas fa-check-circle')
 * @param {Object} options — Opções adicionais
 * @param {string} options.metricLabel — Label da métrica (ex: "Margem: 8.2%")
 * @param {boolean} options.dismissible — Se mostra botão de dismiss
 * @param {number} options.index — Índice para animação escalonada
 */
export function renderAlertCard(type, title, message, icon = '', options = {}) {
    const icons = {
        success: 'fas fa-check-circle',
        warning: 'fas fa-exclamation-triangle',
        danger: 'fas fa-times-circle',
        info: 'fas fa-info-circle',
    };

    const iconClass = icon || icons[type] || icons.info;
    const { metricLabel, dismissible = true, index = 0 } = options;

    const metricHtml = metricLabel
        ? `<span class="alert-metric">${metricLabel}</span>`
        : '';

    const dismissHtml = dismissible
        ? `<button class="alert-dismiss" title="Dispensar" aria-label="Dispensar alerta">
               <i class="fas fa-times"></i>
           </button>`
        : '';

    const animationDelay = index * 0.1;

    return `
        <div class="card alert-card alert-${type}" 
             style="animation-delay: ${animationDelay}s;"
             data-alert-type="${type}">
            <div class="alert-icon">
                <i class="${iconClass}"></i>
            </div>
            <div class="alert-content">
                <div class="alert-header">
                    <h4>${title}</h4>
                    ${metricHtml}
                </div>
                <p>${message}</p>
            </div>
            ${dismissHtml}
        </div>
    `;
}

/**
 * Renderiza uma lista de alertas a partir de um array de AlertDTO.
 * 
 * @param {Array} alerts — Lista de { type, title, message, icon, metricLabel }
 * @returns {string} HTML concatenado de todos os cards
 */
export function renderAlertList(alerts) {
    if (!alerts || alerts.length === 0) {
        return `
            <div class="alert-empty">
                <i class="fas fa-shield-alt"></i>
                <p>Nenhum alerta no momento — tudo está sob controle!</p>
            </div>
        `;
    }

    return alerts.map((alert, index) =>
        renderAlertCard(
            alert.type,
            alert.title,
            alert.message,
            alert.icon,
            {
                metricLabel: alert.metricLabel,
                dismissible: true,
                index
            }
        )
    ).join('');
}

/**
 * Inicializa handlers de dismiss para os alertas renderizados.
 * Deve ser chamada após renderizar os cards no DOM.
 * 
 * @param {HTMLElement} container — Container pai dos alert cards
 */
export function initAlertDismiss(container) {
    if (!container) return;

    container.querySelectorAll('.alert-dismiss').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const card = e.target.closest('.alert-card');
            if (card) {
                card.style.animation = 'alertSlideOut 0.3s ease forwards';
                setTimeout(() => card.remove(), 300);
            }
        });
    });
}

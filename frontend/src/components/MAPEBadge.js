/**
 * Componente MAPEBadge — Gauge visual de precisão do modelo.
 * 
 * Exibe um arco SVG circular com a porcentagem de confiança,
 * cores adaptativas e texto de classificação.
 */

/**
 * Retorna a cor baseada no valor do MAPE.
 * @param {number} mape 
 */
function getMAPEColor(mape) {
    if (mape < 10) return 'var(--color-success, #00c853)';
    if (mape < 20) return 'var(--color-warning-green, #64dd17)';
    if (mape < 50) return 'var(--color-warning, #ffab00)';
    return 'var(--color-danger, #ff1744)';
}

/**
 * Retorna a cor de fundo suave baseada no MAPE.
 */
function getMAPEBgColor(mape) {
    if (mape < 10) return 'rgba(0, 200, 83, 0.1)';
    if (mape < 20) return 'rgba(100, 221, 23, 0.1)';
    if (mape < 50) return 'rgba(255, 171, 0, 0.1)';
    return 'rgba(255, 23, 68, 0.1)';
}

/**
 * Renderiza o badge de MAPE como SVG gauge circular.
 * 
 * @param {Object} accuracy { mape, classification, confidenceScore, totalFeedbacks, bestAlgorithm }
 * @param {'small'|'medium'|'large'} size Tamanho do componente
 */
export function renderMAPEBadge(accuracy, size = 'medium') {
    if (!accuracy || accuracy.totalFeedbacks === 0) {
        return renderEmptyBadge();
    }

    const mape = parseFloat(accuracy.mape) || 0;
    const confidence = parseFloat(accuracy.confidenceScore) || 0;
    const classification = accuracy.classification || 'N/A';
    const totalFeedbacks = accuracy.totalFeedbacks || 0;
    const algorithm = accuracy.bestAlgorithm || 'N/A';

    const color = getMAPEColor(mape);
    const bgColor = getMAPEBgColor(mape);

    // SVG gauge parameters
    const sizes = {
        small: { width: 80, stroke: 6, fontSize: 14, labelSize: 8 },
        medium: { width: 120, stroke: 8, fontSize: 22, labelSize: 10 },
        large: { width: 160, stroke: 10, fontSize: 28, labelSize: 12 }
    };
    const s = sizes[size] || sizes.medium;
    const radius = (s.width - s.stroke) / 2;
    const circumference = 2 * Math.PI * radius;
    const progress = Math.max(0, Math.min(100, confidence)) / 100;
    const dashOffset = circumference * (1 - progress);

    return `
        <div class="mape-badge mape-badge--${size}" style="background: ${bgColor};" 
             title="MAPE: ${mape.toFixed(1)}% | Confiança: ${confidence.toFixed(0)}%">
            <div class="mape-gauge">
                <svg width="${s.width}" height="${s.width}" viewBox="0 0 ${s.width} ${s.width}">
                    <!-- Background circle -->
                    <circle cx="${s.width/2}" cy="${s.width/2}" r="${radius}"
                            fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="${s.stroke}"/>
                    <!-- Progress arc -->
                    <circle cx="${s.width/2}" cy="${s.width/2}" r="${radius}"
                            fill="none" stroke="${color}" stroke-width="${s.stroke}"
                            stroke-linecap="round"
                            stroke-dasharray="${circumference}"
                            stroke-dashoffset="${dashOffset}"
                            transform="rotate(-90, ${s.width/2}, ${s.width/2})"
                            class="mape-gauge-arc"/>
                </svg>
                <div class="mape-gauge-text" style="font-size: ${s.fontSize}px; color: ${color};">
                    ${confidence.toFixed(0)}%
                </div>
            </div>
            <div class="mape-info">
                <span class="mape-classification" style="color: ${color};">${classification}</span>
                <span class="mape-detail">MAPE: ${mape.toFixed(1)}%</span>
                <span class="mape-meta">${totalFeedbacks} feedback${totalFeedbacks !== 1 ? 's' : ''} • ${formatAlgorithm(algorithm)}</span>
            </div>
        </div>
    `;
}

/**
 * Renderiza badge vazio quando não há feedbacks.
 */
function renderEmptyBadge() {
    return `
        <div class="mape-badge mape-badge--empty" title="Submeta feedback para ativar a análise de precisão">
            <div class="mape-gauge">
                <svg width="120" height="120" viewBox="0 0 120 120">
                    <circle cx="60" cy="60" r="52" fill="none" 
                            stroke="rgba(255,255,255,0.08)" stroke-width="8"/>
                </svg>
                <div class="mape-gauge-text" style="font-size: 14px; color: var(--text-secondary);">
                    <i class="fas fa-question"></i>
                </div>
            </div>
            <div class="mape-info">
                <span class="mape-classification" style="color: var(--text-secondary);">Sem dados</span>
                <span class="mape-detail">Submeta feedback para ativar</span>
            </div>
        </div>
    `;
}

/**
 * Renderiza um badge inline compacto (para uso dentro de KPI cards).
 */
export function renderMAPEBadgeInline(accuracy) {
    if (!accuracy || accuracy.totalFeedbacks === 0) {
        return '';
    }

    const mape = parseFloat(accuracy.mape) || 0;
    const confidence = parseFloat(accuracy.confidenceScore) || 0;
    const color = getMAPEColor(mape);

    return `
        <span class="mape-badge-inline" style="color: ${color};" 
              title="Precisão do modelo: ${confidence.toFixed(0)}%">
            <i class="${accuracy.classificationIcon || 'fas fa-bullseye'}"></i>
            ${confidence.toFixed(0)}%
        </span>
    `;
}

/**
 * Formata nome do algoritmo para exibição.
 */
function formatAlgorithm(algorithm) {
    const names = {
        'ARMA': 'ARMA',
        'HOLT_WINTERS': 'Holt-Winters',
        'LINEAR_REGRESSION': 'Regressão Linear',
        'N/A': 'N/A'
    };
    return names[algorithm] || algorithm;
}

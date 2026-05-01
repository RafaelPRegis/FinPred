export function renderSidebar(activeRoute = '/dashboard') {
    const links = [
        { path: '/dashboard', icon: 'fas fa-th-large', label: 'Dashboard' },
        { path: '/products', icon: 'fas fa-box', label: 'Produtos' },
        { path: '/cashflow', icon: 'fas fa-exchange-alt', label: 'Fluxo de Caixa' },
        { path: '/taxes', icon: 'fas fa-calculator', label: 'Calculadora IR' },
        { path: '/acquirers', icon: 'fas fa-credit-card', label: 'Maquininhas' },
        { path: '/simulator', icon: 'fas fa-magic', label: 'Simulador' },
        { path: '/import', icon: 'fas fa-file-upload', label: 'Importação' },
        { path: '/reports', icon: 'fas fa-chart-bar', label: 'Relatórios' },
    ];

    const navItems = links.map(link => `
        <a href="#${link.path}"
           class="sidebar-link ${activeRoute === link.path ? 'active' : ''}"
           id="nav-${link.path.slice(1)}">
            <i class="${link.icon}"></i>
            <span class="sidebar-label">${link.label}</span>
        </a>
    `).join('');

    // Recupera o insight armazenado temporariamente para injetar se disponível.
    // O DashboardPage.js irá atualizar isso ou o backend enviará, mas o HTML principal precisa do slot.
    const insightText = sessionStorage.getItem('dailyInsight') || "Gerando novos insights para seus próximos movimentos...";

    return `
        <aside class="sidebar" id="main-sidebar">
            <div class="sidebar-logo"><i class="fas fa-chart-line"></i></div>
            
            <nav class="sidebar-nav">
                ${navItems}
            </nav>
            
            <div class="sidebar-insight" id="sidebar-insight-container">
                <h5>Insight do dia</h5>
                <p id="sidebar-insight-text">${insightText}</p>
            </div>

            <div class="sidebar-footer" style="flex-direction: column; gap: 8px;">
                <a href="#/profile" class="sidebar-link ${activeRoute === '/profile' ? 'active' : ''}" id="nav-profile">
                    <i class="fas fa-user-circle"></i>
                    <span class="sidebar-label">Minha Conta</span>
                </a>
                <a href="#/" class="sidebar-link" id="nav-logout">
                    <i class="fas fa-sign-out-alt"></i>
                    <span class="sidebar-label">Sair</span>
                </a>
            </div>
        </aside>
    `;
}

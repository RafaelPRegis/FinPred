/**
 * ReportsPage — placeholder para relatórios (Fase 6).
 */
import { renderSidebar } from '../components/Sidebar.js';

export function ReportsPage() {
    return `
        ${renderSidebar('/reports')}
        <main class="app-content">
            <div class="page-header">
                <h1>Relatórios</h1>
                <p>DRE, Fluxo de Caixa Projetado e Análises</p>
            </div>
            <div class="empty-state fade-in-up">
                <i class="fas fa-chart-bar"></i>
                <h3>Relatórios em desenvolvimento</h3>
                <p>Relatórios financeiros detalhados estarão disponíveis nas próximas fases.</p>
                <span class="badge badge-info">Disponível em breve</span>
            </div>
        </main>
    `;
}

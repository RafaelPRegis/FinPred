/**
 * FinPred — Entry Point
 * Inicializa o SPA Router, registra rotas e configura auth guards.
 */
import './styles/base.css';
import './styles/components.css';
import './styles/pages.css';

import { Router } from './utils/router.js';
import { isAuthenticated, clearToken } from './api/client.js';
import { LoginPage, initLoginPage } from './pages/LoginPage.js';
import { RegisterPage, RegisterCompletePage, initRegisterPage, initRegisterCompletePage } from './pages/RegisterPage.js';
import { DashboardPage, initDashboardPage } from './pages/DashboardPage.js';
import { ProductsPage, initProductsPage } from './pages/ProductsPage.js';
import { CashflowPage, initCashflowPage } from './pages/CashflowPage.js';
import { SimulatorPage, initSimulatorPage } from './pages/SimulatorPage.js';
import { ImportPage, initImportPage } from './pages/ImportPage.js';
import { ReportsPage } from './pages/ReportsPage.js';
import { TaxesPage, initTaxesPage } from './pages/TaxesPage.js';
import { AcquirersPage, initAcquirersPage } from './pages/AcquirersPage.js';
import { ProfilePage, initProfilePage } from './pages/ProfilePage.js';
import { showToast } from './components/Toast.js';
import './components/Modal.js';

/** Rotas públicas (não exigem autenticação) */
const PUBLIC_ROUTES = ['/', '/register', '/register/complete'];

/** Rotas que exigem autenticação */
const PROTECTED_ROUTES = ['/dashboard', '/products', '/cashflow', '/taxes', '/acquirers', '/simulator', '/import', '/reports', '/profile'];

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
    const app = document.getElementById('app');
    const loadingScreen = document.getElementById('loading-screen');

    const router = new Router(app);

    // === Rotas públicas ===
    router
        .route('/', () => LoginPage())
        .route('/register', () => RegisterPage())
        .route('/register/complete', () => {
            if (!isAuthenticated()) return LoginPage();
            return RegisterCompletePage();
        });

    // === Rotas protegidas ===
    router
        .route('/dashboard', () => guardRoute(() => DashboardPage()))
        .route('/products', () => guardRoute(() => ProductsPage()))
        .route('/cashflow', () => guardRoute(() => CashflowPage()))
        .route('/taxes', () => guardRoute(() => TaxesPage()))
        .route('/acquirers', () => guardRoute(() => AcquirersPage()))
        .route('/simulator', () => guardRoute(() => SimulatorPage()))
        .route('/import', () => guardRoute(() => ImportPage()))
        .route('/reports', () => guardRoute(() => ReportsPage()))
        .route('/profile', () => guardRoute(() => ProfilePage()));

    // === Inicializar páginas após renderização ===
    window.addEventListener('route-changed', (e) => {
        const path = e.detail.path;

        // Delay mínimo para garantir que o DOM foi renderizado
        requestAnimationFrame(() => {
            switch (path) {
                case '/':
                    initLoginPage();
                    break;
                case '/register':
                    initRegisterPage();
                    break;
                case '/register/complete':
                    initRegisterCompletePage();
                    break;
                case '/dashboard':
                    initDashboardPage();
                    break;
                case '/products':
                    initProductsPage();
                    break;
                case '/cashflow':
                    initCashflowPage();
                    break;
                case '/taxes':
                    initTaxesPage();
                    break;
                case '/acquirers':
                    initAcquirersPage();
                    break;
                case '/import':
                    initImportPage();
                    break;
                case '/simulator':
                    initSimulatorPage();
                    break;
                case '/profile':
                    initProfilePage();
                    break;
            }
        });
    });

    // === Logout global ===
    window.addEventListener('click', (e) => {
        const logoutBtn = e.target.closest('#nav-logout');
        if (logoutBtn) {
            e.preventDefault();
            clearToken();
            window.location.hash = '/';
        }
    });

    // Esconder loading screen e iniciar router
    setTimeout(() => {
        if (loadingScreen) loadingScreen.classList.add('hidden');
        router.start();
    }, 500);

    console.log('🔮 FinPred initialized successfully');
});

/**
 * Route guard: redireciona para login se não autenticado.
 */
function guardRoute(renderFn) {
    if (!isAuthenticated()) {
        // Redirecionar com delay para evitar loop
        setTimeout(() => { window.location.hash = '/'; }, 0);
        return `
            <div class="empty-state">
                <i class="fas fa-lock"></i>
                <h3>Acesso restrito</h3>
                <p>Faça login para acessar esta página.</p>
            </div>
        `;
    }
    return renderFn();
}

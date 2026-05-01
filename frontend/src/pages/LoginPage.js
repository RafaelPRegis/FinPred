/**
 * LoginPage — Página de login com split screen premium.
 * Suporta login por email/senha e Google OAuth2.
 */
import { authApi } from '../api/auth.js';
import { setToken, setUser } from '../api/client.js';

const GOOGLE_CLIENT_ID = '427699906241-mmm6nsl6ggudgfdrgkbncbv00hnkgrie.apps.googleusercontent.com';

export function LoginPage() {
    return `
        <div class="auth-page" id="login-page">
            <div class="auth-panel auth-panel-left">
                <div class="auth-hero fade-in-up" style="background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); padding: 3rem; border-radius: 24px; border: 1px solid rgba(255,255,255,0.08); box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5);">
                    <div class="loading-logo" style="margin-bottom: var(--space-6); justify-content: center; font-size: var(--text-4xl);">
                        <i class="fas fa-chart-line"></i>
                        <span>FinPred</span>
                    </div>
                    <h1 class="gradient-text">Predição Financeira Inteligente</h1>
                    <p style="margin-top: var(--space-4); color: var(--text-secondary);">
                        Gerencie seu comércio, importe dados e receba previsões financeiras
                        precisas com nosso sistema de predição.
                    </p>
                    <div class="auth-features" style="margin-top: var(--space-8); text-align: left;">
                        <div class="auth-feature-item">
                            <i class="fas fa-chart-area" style="color: var(--accent-primary);"></i>
                            <span>Previsões para se mesclar ao seu comércio</span>
                        </div>
                        <div class="auth-feature-item">
                            <i class="fas fa-file-excel" style="color: var(--accent-secondary);"></i>
                            <span>Importação de planilhas CSV/Excel</span>
                        </div>
                        <div class="auth-feature-item">
                            <i class="fas fa-calculator" style="color: var(--accent-warning);"></i>
                            <span>Simulador de cenários financeiros</span>
                        </div>
                        <div class="auth-feature-item">
                            <i class="fas fa-shield-alt" style="color: var(--accent-info);"></i>
                            <span>Dados seguros e criptografados</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="auth-panel">
                <div class="auth-form-container fade-in-up">
                    <h2>Bem-vindo de volta</h2>
                    <p class="subtitle">Entre na sua conta para continuar</p>

                    <div id="login-error" class="auth-error" style="display: none;"></div>

                    <form class="auth-form" id="login-form">
                        <div class="input-group">
                            <label for="login-email">E-mail</label>
                            <div class="input-icon">
                                <i class="fas fa-envelope"></i>
                                <input type="email" id="login-email" class="input"
                                       placeholder="seu@email.com" required autocomplete="email" />
                            </div>
                        </div>
                        <div class="input-group">
                            <label for="login-password">Senha</label>
                            <div class="input-icon">
                                <i class="fas fa-lock"></i>
                                <input type="password" id="login-password" class="input"
                                       placeholder="••••••••" required autocomplete="current-password" />
                            </div>
                        </div>
                        <button type="submit" class="btn btn-primary btn-lg" id="btn-login">
                            <i class="fas fa-sign-in-alt"></i>
                            <span>Entrar</span>
                        </button>
                    </form>

                    <div class="auth-divider">
                        <span>ou</span>
                    </div>

                    <div id="google-signin-btn" class="google-btn-wrapper"></div>

                    <p class="auth-switch">
                        Não tem conta? <a href="#/register">Cadastre-se gratuitamente</a>
                    </p>
                </div>
            </div>
        </div>
    `;
}

/**
 * Inicializa os event listeners da página de login.
 */
export function initLoginPage() {
    const form = document.getElementById('login-form');
    if (!form) return;

    // Login com email/senha
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btn-login');
        const errorDiv = document.getElementById('login-error');

        const email = document.getElementById('login-email').value.trim();
        const password = document.getElementById('login-password').value;

        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Entrando...</span>';
        errorDiv.style.display = 'none';

        try {
            const response = await authApi.login({ email, password });
            handleAuthSuccess(response);
        } catch (error) {
            showError(errorDiv, error.message);
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-sign-in-alt"></i> <span>Entrar</span>';
        }
    });

    // Google Sign-In
    initGoogleSignIn();
}

/**
 * Inicializa o botão de login do Google.
 */
function initGoogleSignIn() {
    const container = document.getElementById('google-signin-btn');
    if (!container || typeof google === 'undefined') {
        // Google script ainda carregando — retry
        if (container) {
            container.innerHTML = `
                <button class="btn btn-secondary btn-lg" style="width: 100%;" onclick="window.location.reload()">
                    <i class="fab fa-google"></i> Entrar com Google
                </button>
            `;
        }
        return;
    }

    try {
        google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleGoogleResponse,
            auto_select: false,
        });

        google.accounts.id.renderButton(container, {
            theme: 'filled_black',
            size: 'large',
            width: '100%',
            text: 'signin_with',
            shape: 'rectangular',
            locale: 'pt-BR',
        });
    } catch (err) {
        console.warn('Google Sign-In não disponível:', err);
        container.innerHTML = `
            <button class="btn btn-secondary btn-lg" style="width: 100%;" disabled>
                <i class="fab fa-google"></i> Google indisponível
            </button>
        `;
    }
}

/**
 * Callback do Google Sign-In.
 */
async function handleGoogleResponse(response) {
    const errorDiv = document.getElementById('login-error');
    try {
        const authResponse = await authApi.googleLogin({ credential: response.credential });
        handleAuthSuccess(authResponse);
    } catch (error) {
        showError(errorDiv, error.message);
    }
}

// Expor globalmente para o callback do Google
window.handleGoogleResponse = handleGoogleResponse;

/**
 * Processa login bem-sucedido.
 */
function handleAuthSuccess(response) {
    setToken(response.token);
    setUser({
        id: response.userId,
        email: response.email,
        username: response.username,
        profileComplete: response.profileComplete,
    });

    if (!response.profileComplete) {
        // Redirecionar para completar perfil (etapa 2)
        window.location.hash = '/register/complete';
    } else {
        window.location.hash = '/dashboard';
    }
}

function showError(element, message) {
    if (element) {
        element.textContent = message;
        element.style.display = 'block';
    }
}

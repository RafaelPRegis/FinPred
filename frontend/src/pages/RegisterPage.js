/**
 * RegisterPage — Cadastro em 2 etapas.
 * Etapa 1: email, nome, nome da empresa, senha
 * Etapa 2: área do comércio (businessType) e natureza jurídica
 */
import { authApi } from '../api/auth.js';
import { setToken, setUser } from '../api/client.js';

export function RegisterPage() {
    return `
        <div class="auth-page" id="register-page">
            <div class="auth-panel auth-panel-left">
                <div class="auth-hero fade-in-up">
                    <div class="loading-logo" style="margin-bottom: var(--space-6); justify-content: center; font-size: var(--text-4xl);">
                        <i class="fas fa-chart-line"></i>
                        <span>FinPred</span>
                    </div>
                    <h1 class="gradient-text">Comece Gratuitamente</h1>
                    <p style="margin-top: var(--space-4);">
                        Cadastre-se e descubra o potencial financeiro do seu negócio
                        com previsões inteligentes.
                    </p>

                    <!-- Progress steps -->
                    <div class="register-steps" style="margin-top: var(--space-8);">
                        <div class="step-indicator active" id="step-ind-1">
                            <div class="step-number">1</div>
                            <span>Seus dados</span>
                        </div>
                        <div class="step-line"></div>
                        <div class="step-indicator" id="step-ind-2">
                            <div class="step-number">2</div>
                            <span>Seu negócio</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="auth-panel">
                <div class="auth-form-container fade-in-up">
                    <!-- STEP 1 -->
                    <div id="register-step-1">
                        <h2>Criar Conta</h2>
                        <p class="subtitle">Preencha seus dados para começar</p>

                        <div id="register-error-1" class="auth-error" style="display: none;"></div>

                        <form class="auth-form" id="register-form-1">
                            <div class="input-group">
                                <label for="reg-name">Nome completo</label>
                                <div class="input-icon">
                                    <i class="fas fa-user"></i>
                                    <input type="text" id="reg-name" class="input"
                                           placeholder="João Silva" required autocomplete="name" />
                                </div>
                            </div>
                            <div class="input-group">
                                <label for="reg-email">E-mail</label>
                                <div class="input-icon">
                                    <i class="fas fa-envelope"></i>
                                    <input type="email" id="reg-email" class="input"
                                           placeholder="seu@email.com" required autocomplete="email" />
                                </div>
                            </div>
                            <div class="input-group">
                                <label for="reg-company">Nome da empresa</label>
                                <div class="input-icon">
                                    <i class="fas fa-building"></i>
                                    <input type="text" id="reg-company" class="input"
                                           placeholder="Minha Empresa LTDA" required />
                                </div>
                            </div>
                            <div class="input-group">
                                <label for="reg-password">Senha</label>
                                <div class="input-icon">
                                    <i class="fas fa-lock"></i>
                                    <input type="password" id="reg-password" class="input"
                                           placeholder="Mínimo 8 caracteres" required minlength="8" autocomplete="new-password" />
                                </div>
                            </div>
                            <button type="submit" class="btn btn-primary btn-lg" id="btn-step1">
                                <span>Continuar</span>
                                <i class="fas fa-arrow-right"></i>
                            </button>
                        </form>

                        <p class="auth-switch">
                            Já tem conta? <a href="#/">Fazer login</a>
                        </p>
                    </div>

                    <!-- STEP 2 -->
                    <div id="register-step-2" style="display: none;">
                        <h2>Sobre seu negócio</h2>
                        <p class="subtitle">Essas informações nos ajudam a personalizar suas previsões</p>

                        <div id="register-error-2" class="auth-error" style="display: none;"></div>

                        <form class="auth-form" id="register-form-2">
                            <div class="input-group">
                                <label>Área do comércio</label>
                                <div class="business-type-grid" id="business-type-grid">
                                    <button type="button" class="business-type-option" data-value="INDUSTRIA">
                                        <i class="fas fa-industry"></i>
                                        <span>Indústria</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="VAREJO">
                                        <i class="fas fa-store"></i>
                                        <span>Varejo</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="EDUCACAO">
                                        <i class="fas fa-graduation-cap"></i>
                                        <span>Educação</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="SERVICO">
                                        <i class="fas fa-concierge-bell"></i>
                                        <span>Serviços</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="TECNOLOGIA">
                                        <i class="fas fa-laptop-code"></i>
                                        <span>Tecnologia</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="SAUDE">
                                        <i class="fas fa-heartbeat"></i>
                                        <span>Saúde</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="ALIMENTACAO">
                                        <i class="fas fa-utensils"></i>
                                        <span>Alimentação</span>
                                    </button>
                                    <button type="button" class="business-type-option" data-value="VESTUARIO">
                                        <i class="fas fa-tshirt"></i>
                                        <span>Vestuário</span>
                                    </button>
                                </div>
                                <input type="hidden" id="reg-business-type" required />
                            </div>

                            <div class="input-group">
                                <label for="reg-legal-nature">Natureza jurídica</label>
                                <select id="reg-legal-nature" class="input select" required>
                                    <option value="" disabled selected>Selecione...</option>
                                    <option value="MEI">MEI — Microempreendedor Individual</option>
                                    <option value="ME">ME — Microempresa</option>
                                    <option value="EPP">EPP — Empresa de Pequeno Porte</option>
                                    <option value="LTDA">LTDA — Sociedade Limitada</option>
                                    <option value="SA">SA — Sociedade Anônima</option>
                                    <option value="EIRELI">EIRELI — Empresa Individual</option>
                                </select>
                            </div>

                            <div style="display: flex; gap: var(--space-3);">
                                <button type="button" class="btn btn-secondary btn-lg" id="btn-back-step1"
                                        style="flex: 0 0 auto;">
                                    <i class="fas fa-arrow-left"></i>
                                </button>
                                <button type="submit" class="btn btn-success btn-lg" id="btn-step2"
                                        style="flex: 1;">
                                    <i class="fas fa-check"></i>
                                    <span>Concluir Cadastro</span>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Página de completar registro (para usuários Google que pularam etapa 2).
 */
export function RegisterCompletePage() {
    // Reutiliza a mesma UI da etapa 2 mas em contexto independente
    return `
        <div class="auth-page" id="register-complete-page">
            <div class="auth-panel auth-panel-left">
                <div class="auth-hero fade-in-up">
                    <div class="loading-logo" style="margin-bottom: var(--space-6); justify-content: center; font-size: var(--text-4xl);">
                        <i class="fas fa-chart-line"></i>
                        <span>FinPred</span>
                    </div>
                    <h1 class="gradient-text">Quase lá!</h1>
                    <p style="margin-top: var(--space-4);">
                        Complete seu perfil para que possamos personalizar suas previsões financeiras.
                    </p>
                </div>
            </div>
            <div class="auth-panel">
                <div class="auth-form-container fade-in-up">
                    <h2>Sobre seu negócio</h2>
                    <p class="subtitle">Essas informações são essenciais para gerar previsões precisas</p>

                    <div id="complete-error" class="auth-error" style="display: none;"></div>

                    <form class="auth-form" id="complete-form">
                        <div class="input-group">
                            <label>Área do comércio</label>
                            <div class="business-type-grid" id="complete-business-grid">
                                <button type="button" class="business-type-option" data-value="INDUSTRIA">
                                    <i class="fas fa-industry"></i><span>Indústria</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="VAREJO">
                                    <i class="fas fa-store"></i><span>Varejo</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="EDUCACAO">
                                    <i class="fas fa-graduation-cap"></i><span>Educação</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="SERVICO">
                                    <i class="fas fa-concierge-bell"></i><span>Serviços</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="TECNOLOGIA">
                                    <i class="fas fa-laptop-code"></i><span>Tecnologia</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="SAUDE">
                                    <i class="fas fa-heartbeat"></i><span>Saúde</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="ALIMENTACAO">
                                    <i class="fas fa-utensils"></i><span>Alimentação</span>
                                </button>
                                <button type="button" class="business-type-option" data-value="VESTUARIO">
                                    <i class="fas fa-tshirt"></i><span>Vestuário</span>
                                </button>
                            </div>
                            <input type="hidden" id="complete-business-type" required />
                        </div>

                        <div class="input-group">
                            <label for="complete-legal-nature">Natureza jurídica</label>
                            <select id="complete-legal-nature" class="input select" required>
                                <option value="" disabled selected>Selecione...</option>
                                <option value="MEI">MEI — Microempreendedor Individual</option>
                                <option value="ME">ME — Microempresa</option>
                                <option value="EPP">EPP — Empresa de Pequeno Porte</option>
                                <option value="LTDA">LTDA — Sociedade Limitada</option>
                                <option value="SA">SA — Sociedade Anônima</option>
                                <option value="EIRELI">EIRELI — Empresa Individual</option>
                            </select>
                        </div>

                        <button type="submit" class="btn btn-success btn-lg" id="btn-complete">
                            <i class="fas fa-check"></i>
                            <span>Concluir e Acessar</span>
                        </button>
                    </form>
                </div>
            </div>
        </div>
    `;
}

/**
 * Inicializa a página de registro (2 etapas).
 */
export function initRegisterPage() {
    const form1 = document.getElementById('register-form-1');
    const form2 = document.getElementById('register-form-2');

    if (form1) initStep1(form1);
    if (form2) initStep2(form2);
    initBusinessTypeGrid('business-type-grid', 'reg-business-type');

    // Botão voltar
    const btnBack = document.getElementById('btn-back-step1');
    if (btnBack) {
        btnBack.addEventListener('click', () => {
            document.getElementById('register-step-1').style.display = 'block';
            document.getElementById('register-step-2').style.display = 'none';
            updateStepIndicators(1);
        });
    }
}

/**
 * Inicializa a página de completar registro (Google users).
 */
export function initRegisterCompletePage() {
    const form = document.getElementById('complete-form');
    if (!form) return;

    initBusinessTypeGrid('complete-business-grid', 'complete-business-type');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btn-complete');
        const errorDiv = document.getElementById('complete-error');

        const businessType = document.getElementById('complete-business-type').value;
        const legalNature = document.getElementById('complete-legal-nature').value;

        if (!businessType) {
            showError(errorDiv, 'Selecione a área do seu comércio');
            return;
        }

        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Finalizando...</span>';
        errorDiv.style.display = 'none';

        try {
            await authApi.registerStepTwo({ businessType, legalNature });
            window.location.hash = '/dashboard';
        } catch (error) {
            showError(errorDiv, error.message);
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check"></i> <span>Concluir e Acessar</span>';
        }
    });
}

function initStep1(form) {
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btn-step1');
        const errorDiv = document.getElementById('register-error-1');

        const username = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const companyName = document.getElementById('reg-company').value.trim();
        const password = document.getElementById('reg-password').value;

        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Registrando...</span>';
        errorDiv.style.display = 'none';

        try {
            const response = await authApi.registerStepOne({ email, username, companyName, password });

            // Salvar token para usar na etapa 2
            setToken(response.token);
            setUser({
                id: response.userId,
                email: response.email,
                username: response.username,
            });

            // Avançar para etapa 2
            document.getElementById('register-step-1').style.display = 'none';
            document.getElementById('register-step-2').style.display = 'block';
            updateStepIndicators(2);

        } catch (error) {
            showError(errorDiv, error.message);
            btn.disabled = false;
            btn.innerHTML = '<span>Continuar</span> <i class="fas fa-arrow-right"></i>';
        }
    });
}

function initStep2(form) {
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('btn-step2');
        const errorDiv = document.getElementById('register-error-2');

        const businessType = document.getElementById('reg-business-type').value;
        const legalNature = document.getElementById('reg-legal-nature').value;

        if (!businessType) {
            showError(errorDiv, 'Selecione a área do seu comércio');
            return;
        }
        if (!legalNature) {
            showError(errorDiv, 'Selecione a natureza jurídica');
            return;
        }

        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Finalizando...</span>';
        errorDiv.style.display = 'none';

        try {
            await authApi.registerStepTwo({ businessType, legalNature });
            window.location.hash = '/dashboard';
        } catch (error) {
            showError(errorDiv, error.message);
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check"></i> <span>Concluir Cadastro</span>';
        }
    });
}

/**
 * Inicializa a grid de seleção do tipo de negócio.
 */
function initBusinessTypeGrid(gridId, hiddenInputId) {
    const grid = document.getElementById(gridId);
    if (!grid) return;

    grid.addEventListener('click', (e) => {
        const option = e.target.closest('.business-type-option');
        if (!option) return;

        // Remover seleção anterior
        grid.querySelectorAll('.business-type-option').forEach(opt =>
            opt.classList.remove('selected')
        );

        // Selecionar
        option.classList.add('selected');
        document.getElementById(hiddenInputId).value = option.dataset.value;
    });
}

function updateStepIndicators(step) {
    const ind1 = document.getElementById('step-ind-1');
    const ind2 = document.getElementById('step-ind-2');
    if (step === 2) {
        ind1?.classList.remove('active');
        ind1?.classList.add('completed');
        ind2?.classList.add('active');
    } else {
        ind1?.classList.add('active');
        ind1?.classList.remove('completed');
        ind2?.classList.remove('active');
    }
}

function showError(element, message) {
    if (element) {
        element.textContent = message;
        element.style.display = 'block';
    }
}

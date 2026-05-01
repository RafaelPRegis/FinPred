import { renderSidebar } from '../components/Sidebar.js';
import { api } from '../api/client.js';
import { showToast } from '../components/Toast.js';

export function ProfilePage() {
    return `
        ${renderSidebar('/profile')}
        <main class="app-content">
            <div class="page-header" style="margin-bottom: var(--space-6);">
                <h1 style="font-size: 1.5rem; font-weight: 500; color: var(--text-primary);">Minha Conta</h1>
                <p style="color: var(--text-secondary);">Gerencie suas informações de perfil e do seu negócio.</p>
            </div>

            <div class="card fade-in-up" style="max-width: 600px; padding: var(--space-6);">
                <form id="profile-form" style="display: flex; flex-direction: column; gap: var(--space-5);">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4);">
                        <div class="form-group">
                            <label for="profile-username" class="form-label">Nome Completo</label>
                            <input type="text" id="profile-username" class="form-input" required>
                        </div>
                        <div class="form-group">
                            <label for="profile-email" class="form-label">E-mail</label>
                            <input type="email" id="profile-email" class="form-input" disabled style="opacity: 0.7; cursor: not-allowed;">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="profile-company" class="form-label">Nome da Empresa</label>
                        <input type="text" id="profile-company" class="form-input" required>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4);">
                        <div class="form-group">
                            <label for="profile-nature" class="form-label">Natureza Jurídica</label>
                            <select id="profile-nature" class="form-input select" required>
                                <option value="MEI">MEI (Microempreendedor Individual)</option>
                                <option value="ME">ME (Microempresa)</option>
                                <option value="EPP">EPP (Empresa de Pequeno Porte)</option>
                                <option value="LTDA">LTDA (Sociedade Limitada)</option>
                                <option value="SA">S.A. (Sociedade Anônima)</option>
                                <option value="OUTROS">Outros</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="profile-business" class="form-label">Setor de Atuação</label>
                            <select id="profile-business" class="form-input select" required>
                                <option value="SERVICOS">Serviços</option>
                                <option value="COMERCIO">Comércio / Varejo</option>
                                <option value="INDUSTRIA">Indústria</option>
                                <option value="TECNOLOGIA">Tecnologia (SaaS, TI)</option>
                                <option value="SAUDE">Saúde / Bem-estar</option>
                                <option value="EDUCACAO">Educação</option>
                                <option value="ALIMENTACAO">Alimentação / Restaurantes</option>
                                <option value="OUTROS">Outros</option>
                            </select>
                        </div>
                    </div>

                    <div class="auth-divider" style="margin: var(--space-2) 0;">
                        <span>Segurança</span>
                    </div>

                    <div class="form-group">
                        <label for="profile-password" class="form-label">Nova Senha</label>
                        <input type="password" id="profile-password" class="form-input" placeholder="Deixe em branco para não alterar">
                        <small style="color: var(--text-muted); margin-top: 4px; display: block;">Se fez login via Google, não é possível alterar a senha aqui.</small>
                    </div>

                    <div style="display: flex; justify-content: flex-end; margin-top: var(--space-4);">
                        <button type="submit" class="btn btn-primary" id="profile-submit-btn">
                            Salvar Alterações
                        </button>
                    </div>
                </form>
            </div>
        </main>
    `;
}

export async function initProfilePage() {
    try {
        const profile = await api.get('/auth/profile');
        
        document.getElementById('profile-username').value = profile.username || '';
        document.getElementById('profile-email').value = profile.email || '';
        document.getElementById('profile-company').value = profile.companyName || '';
        document.getElementById('profile-nature').value = profile.legalNature || 'MEI';
        document.getElementById('profile-business').value = profile.businessType || 'SERVICOS';
        
        // Desabilitar o campo de senha se o provedor for o Google
        if (profile.provider === 'GOOGLE') {
            const pwdInput = document.getElementById('profile-password');
            pwdInput.disabled = true;
            pwdInput.placeholder = "Indisponível (Login via Google)";
            pwdInput.style.opacity = '0.7';
            pwdInput.style.cursor = 'not-allowed';
        }

        setupFormSubmit(profile.id);

    } catch (error) {
        showToast('Erro ao carregar dados do perfil', 'error');
    }
}

function setupFormSubmit(userId) {
    const form = document.getElementById('profile-form');
    const submitBtn = document.getElementById('profile-submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const data = {
            username: document.getElementById('profile-username').value,
            companyName: document.getElementById('profile-company').value,
            legalNature: document.getElementById('profile-nature').value,
            businessType: document.getElementById('profile-business').value,
            password: document.getElementById('profile-password').value
        };

        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Salvando...';
        submitBtn.disabled = true;

        try {
            await api.put('/auth/profile', data);
            showToast('Perfil atualizado com sucesso!', 'success');
            document.getElementById('profile-password').value = ''; // Limpar o campo de senha após sucesso
        } catch (error) {
            showToast(error.message || 'Erro ao atualizar perfil', 'error');
        } finally {
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
    });
}

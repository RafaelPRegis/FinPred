/**
 * SPA Router simples baseado em hash.
 * Gerencia navegação entre páginas sem recarregar.
 */
export class Router {
    constructor(appElement) {
        this.routes = {};
        this.appElement = appElement;
        this.currentRoute = null;

        window.addEventListener('hashchange', () => this.resolve());
    }

    /**
     * Registra uma rota.
     * @param {string} path - Hash path (ex: '/dashboard')
     * @param {Function} handler - Função que retorna o HTML da página
     */
    route(path, handler) {
        this.routes[path] = handler;
        return this;
    }

    /**
     * Resolve a rota atual baseada no hash da URL.
     */
    async resolve() {
        const hash = window.location.hash.slice(1) || '/';
        const route = this.routes[hash];

        if (route) {
            this.currentRoute = hash;
            try {
                const content = await route();
                this.appElement.innerHTML = content;
                // Disparar evento customizado para que as páginas possam inicializar
                window.dispatchEvent(new CustomEvent('route-changed', { detail: { path: hash } }));
            } catch (error) {
                console.error(`Erro ao renderizar rota ${hash}:`, error);
                this.appElement.innerHTML = this.errorPage(error);
            }
        } else {
            // Fallback: redireciona para login
            this.navigate('/');
        }
    }

    /**
     * Navega para uma rota.
     * @param {string} path - Hash path
     */
    navigate(path) {
        window.location.hash = path;
    }

    /**
     * Retorna a rota atual.
     */
    getCurrentRoute() {
        return this.currentRoute;
    }

    /**
     * Página de erro genérica.
     */
    errorPage(error) {
        return `
            <div class="empty-state">
                <i class="fas fa-exclamation-triangle"></i>
                <h3>Erro ao carregar página</h3>
                <p>${error.message || 'Ocorreu um erro inesperado.'}</p>
                <button class="btn btn-primary" onclick="window.location.hash='/'">
                    <i class="fas fa-home"></i> Voltar ao Início
                </button>
            </div>
        `;
    }

    /**
     * Inicializa o router (resolve rota inicial).
     */
    start() {
        this.resolve();
    }
}

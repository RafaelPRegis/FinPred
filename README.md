# 🔮 FinPred — Sistema de Predição Financeira Distribuído

Sistema inteligente de gestão e predição financeira para pequenos e médios comércios brasileiros, construído com arquitetura de microsserviços.

## 🏗️ Arquitetura

```
Frontend (Vite + Vanilla JS)  →  API Gateway (:8080)
                                     ├── Auth Service    (:8081)
                                     ├── Core Service    (:8082)
                                     └── Prediction Svc  (:8083)
```

## 🚀 Stack Tecnológica

| Camada | Tecnologias |
|:---|:---|
| **Backend** | Java 21+, Spring Boot 3.3, Spring Cloud Gateway, JWT (RS256) |
| **Frontend** | Vite, Vanilla JS (ES Modules), Chart.js, CSS Variables |
| **Dados** | H2 (dev) / MySQL 8 (prod), RabbitMQ |
| **Infra** | Docker Compose, Maven Wrapper |

## 📋 Pré-requisitos

- **Java 21+** (JDK instalado)
- **Node.js 20+** e **npm**
- **Docker** (opcional — apenas para perfil de produção)

## ⚡ Início Rápido

### 1. Clonar e configurar variáveis de ambiente

```bash
git clone <url-do-repositorio>
cd projeto_finpred
cp .env.example .env
# Edite o .env com suas chaves
```

### 2. Subir todos os serviços (modo desenvolvimento)

```powershell
.\start_services.ps1
```

Este script inicia todos os microsserviços com perfil `dev` (H2 em memória) e o frontend simultaneamente.

### 3. Acessar a aplicação

- **Frontend**: http://localhost:5173
- **API Gateway**: http://localhost:8080
- **RabbitMQ Management** (se Docker ativo): http://localhost:15672

## 🗂️ Estrutura do Projeto

```
projeto_finpred/
├── backend/
│   ├── api-gateway/        # Roteamento, JWT Filter, CORS
│   ├── auth-service/       # Autenticação, OAuth2, JWT
│   ├── core-service/       # Produtos, Transações, Importação
│   └── prediction-service/ # Motor preditivo, Simulador
├── frontend/               # SPA com Vite + Vanilla JS
├── docker-compose.yml      # MySQL + RabbitMQ
├── start_services.ps1      # Script para subir tudo
└── .env.example            # Template de variáveis
```

## 🔧 Comandos Úteis

```bash
# Rodar um serviço individual
cd backend/auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Rodar testes
./mvnw test

# Frontend
cd frontend
npm run dev

# Docker (produção)
docker-compose up -d
```

## 📊 Funcionalidades

- ✅ Autenticação JWT + Google OAuth2
- ✅ CRUD de Produtos e Transações
- ✅ Importação de dados via CSV/Excel
- ✅ Motor de predição com Regressão Linear + Sazonalidade
- ✅ Simulador interativo com cenários (pessimista/neutro/otimista)
- ✅ Dashboard com KPIs e gráficos
- ✅ Cálculo de IR por regime tributário
- ✅ Sistema de feedback e ajuste automático (MAPE)

## 📄 Licença

Projeto privado — uso educacional e comercial restrito.

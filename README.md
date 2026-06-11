# FinPred — Sistema de Predição Financeira Distribuído

O FinPred é uma aplicação web voltada para gestão financeira e apoio à tomada de decisão em pequenos negócios. A proposta do sistema é reunir controle de produtos, transações, fluxo de caixa, dashboard, alertas, simulação tributária, taxas de adquirentes e previsão de cenários financeiros em uma única plataforma. 

## Arquiterura

O frontend acessa o backend por meio da rota base /api. O API Gateway expõe a porta 8080, aplica regras de CORS, valida JWT nas rotas protegidas e redireciona as chamadas para Auth Service, Core Service ou Prediction Service.

## Estrutura de diretorios


## Tecnologias usadas

### Backend
- Java 21
- Spring Boot 3.2.0
- Maven Wrapper

### Frontend
- React 18
- Vite

### Infraestrutura
- Docker Compose
- MySQL
- RabbitMQ

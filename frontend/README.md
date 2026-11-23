# Frontend — Sistema de Estoque e Vendas

Interface web do sistema (Pessoa 1). Feita em **React + TypeScript + Vite** e servida em produção por **Nginx**.

## Telas

| Rota | Tela | O que faz |
|---|---|---|
| `/` | Dashboard | Faturamento do dia e do mês, produtos com estoque baixo, gráfico dos últimos 7 dias e últimas vendas (atualiza sozinho a cada 30 s) |
| `/produtos` | Produtos | Cadastro, edição, remoção e listagem com busca e filtros (categoria, estoque baixo, sem estoque) |
| `/entradas` | Entrada de mercadorias | Registra a chegada de produtos (soma no estoque) e mostra o histórico |
| `/vendas/nova` | Nova venda | Carrinho com vários produtos, forma de pagamento e validação de estoque |
| `/vendas` | Histórico de vendas | Vendas por período, com os itens de cada venda |
| `/reposicoes` | Reposição | Produtos sem estoque detectados pelo Worker (atualiza a cada 15 s) |
| `/relatorios` | Relatórios | Faturamento, quantidade de vendas, ticket médio, faturamento por dia e por forma de pagamento, produtos mais vendidos |

O rodapé do menu mostra a **versão do frontend** e a **versão + nome do Pod da API** (vem de `GET /api/info`). Isso ajuda nas demonstrações do Kubernetes (balanceamento entre réplicas e atualização v1 → v2).

## Como a interface fala com a API

O frontend chama sempre caminhos relativos (`/api/...`), então não há endereço de API fixo no código e não há problema de CORS:

- **Desenvolvimento:** o Vite repassa `/api` para `http://localhost:8080` (ou para o valor de `API_URL`).
- **Docker / Kubernetes:** o Nginx do container repassa `/api/` para a variável de ambiente `API_URL` (padrão `http://api:8080`).

## Rodar em desenvolvimento

Requisitos: Node 20+ e a API rodando na porta 8080.

```bash
cd frontend
npm install
npm run dev
```

Acesse http://localhost:5173.

Se a API estiver em outro endereço:

```bash
API_URL=http://localhost:9090 npm run dev
```

## Docker

```bash
# build (v1)
docker build -t <usuario-dockerhub>/estoque-frontend:v1 frontend

# build da v2 (para a demonstração de atualização)
docker build --build-arg APP_VERSION=v2 -t <usuario-dockerhub>/estoque-frontend:v2 frontend

# rodar apontando para uma API
docker run -p 3000:80 -e API_URL=http://api:8080 <usuario-dockerhub>/estoque-frontend:v1
```

| Variável | Quando | Padrão | Para quê |
|---|---|---|---|
| `APP_VERSION` | build (`--build-arg`) | `v1` | Versão mostrada no rodapé do menu |
| `API_URL` | execução (`-e` / `env` do Pod) | `http://api:8080` | Endereço da API para o proxy do Nginx |

## Notas para o Docker Compose / Kubernetes (Pessoa 4)

- O container escuta na **porta 80**.
- O nome do serviço da API deve bater com `API_URL` (por padrão o serviço se chama `api` na porta `8080`).
- Healthcheck: `GET /healthz` devolve `200 ok` (serve para `livenessProbe`/`readinessProbe`).
- Para expor no cluster, um Service `NodePort` ou `LoadBalancer` apontando para a porta 80.

Exemplo de serviço no `docker-compose.yml`:

```yaml
frontend:
  image: <usuario-dockerhub>/estoque-frontend:v1
  ports:
    - "3000:80"
  environment:
    API_URL: http://api:8080
  depends_on:
    - api
```

## Estrutura

```
frontend/
├── Dockerfile                  # build multi-stage (Node -> Nginx)
├── nginx/default.conf.template # proxy /api + fallback da SPA
├── index.html
├── vite.config.ts
└── src/
    ├── api/          # cliente HTTP e tipos da API
    ├── components/   # layout, modal, avisos, gráficos, componentes de UI
    ├── pages/        # uma tela por arquivo
    ├── utils/        # formatação e hook de carregamento
    ├── App.tsx       # rotas
    └── styles.css
```

# Worker - Estoque e Vendas

Serviço separado da API (Java 21 + Spring Boot) que roda sozinho **a cada minuto** e:

1. Verifica todos os produtos cadastrados no MongoDB.
2. Identifica os produtos ativos com **estoque igual a zero** (ou menor).
3. Registra esses produtos na lista de reposição (coleção `reposicao`, status `PENDENTE`).
4. Recalcula o resumo dos relatórios (**quantidade total de vendas** e **faturamento total**) e grava na coleção `relatorios`.

O Worker não recebe requisições do frontend. Ele só lê e grava no MongoDB, o mesmo banco da API.

```
            +-----------+        +-------+
Frontend -> |    API    | -----> |       |
            +-----------+        |MongoDB|
            +-----------+        |       |
            |  Worker   | -----> |       |   (a cada 1 minuto)
            +-----------+        +-------+
```

## Como funciona

| Coleção | O que o Worker faz |
|---|---|
| `produtos` | Lê. Busca `ativo != false` e `quantidadeEstoque <= 0`. |
| `reposicao` | Cria um documento `PENDENTE` por produto zerado. Se já existe um `PENDENTE` para o produto, só atualiza nome e quantidade (não duplica). |
| `vendas` | Lê. Soma quantidade de vendas, faturamento (`valorTotal`) e itens vendidos. |
| `relatorios` | Grava o resumo geral no documento `_id: "resumo-geral"`. |

Quando chega uma entrada de mercadoria, **a API** muda a reposição para `RESOLVIDA`. Se o produto zerar de novo depois disso, o Worker cria uma nova reposição `PENDENTE`. O contrato completo está em [`api/README.md`](../api/README.md#contrato-com-o-worker-pessoa-3).

Documento gravado em `reposicao` (o mesmo formato que a API lê em `GET /api/reposicoes`):

```json
{
  "produtoId": "6740f1c2a9e4b25d8c3e1a01",
  "produtoNome": "Arroz 5kg",
  "quantidadeAtual": 0,
  "status": "PENDENTE",
  "dataRegistro": "2026-11-23T15:00:00Z"
}
```

Documento gravado em `relatorios`:

```json
{
  "_id": "resumo-geral",
  "quantidadeVendas": 15,
  "faturamentoTotal": 432.10,
  "quantidadeItensVendidos": 48,
  "ticketMedio": 28.81,
  "totalProdutosAtivos": 12,
  "produtosSemEstoque": 2,
  "reposicoesPendentes": 2,
  "produtosParaRepor": [ { "produtoId": "...", "produtoNome": "Arroz 5kg", "quantidadeAtual": 0 } ],
  "atualizadoEm": "2026-11-23T15:00:00Z"
}
```

## Estrutura

```
worker/
├── Dockerfile
├── pom.xml
└── src/main/java/br/com/estoque/worker/
    ├── EstoqueWorkerApplication.java       # @EnableScheduling
    ├── job/VerificacaoEstoqueJob.java      # @Scheduled a cada 60 s, logs e tratamento de erro
    ├── service/VerificacaoEstoqueService.java  # produtos zerados -> reposicoes
    ├── service/ResumoRelatorioService.java     # totais de vendas -> relatorios
    ├── controller/StatusController.java    # GET /status
    └── model/                               # records com os resultados
```

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/estoque` (no Docker: `mongodb://mongo:27017/estoque`) | Conexão com o banco |
| `WORKER_INTERVALO_MS` | `60000` | Intervalo entre verificações (1 minuto) |
| `WORKER_ATRASO_INICIAL_MS` | `5000` | Espera antes da primeira verificação |
| `SERVER_PORT` | `8081` | Porta do `/status` e do `/actuator/health` |

## Executar

### Local (precisa de Java 21 e de um MongoDB em `localhost:27017`)

```bash
cd worker
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

### Testes

Usam um MongoDB embutido, então não precisam de Docker:

```bash
./mvnw test
```

### Docker

```bash
docker build -t <usuario-dockerhub>/estoque-worker:v1 ./worker
docker run --rm -e MONGODB_URI=mongodb://mongo:27017/estoque --network <rede-do-mongo> <usuario-dockerhub>/estoque-worker:v1
```

Trecho para o `docker-compose.yml`:

```yaml
  worker:
    build: ./worker
    image: <usuario-dockerhub>/estoque-worker:v1
    environment:
      MONGODB_URI: mongodb://mongo:27017/estoque
    depends_on:
      - mongo
    restart: unless-stopped
```

No Kubernetes: Deployment com **1 réplica**, mesma variável `MONGODB_URI`. Ele não precisa de Service (ninguém chama o Worker), mas as probes podem usar `/actuator/health/liveness` e `/actuator/health/readiness` na porta 8081.

## Como mostrar na apresentação

1. Logs do Worker, uma linha por minuto:
   ```
   docker compose logs -f worker        # ou: kubectl logs -f deploy/worker
   ```
   ```
   Reposicao registrada: Arroz 5kg (estoque 0)
   Verificacao #3: 1 produto(s) sem estoque, 1 nova(s) reposicao(oes), 1 pendente(s) | vendas: 15, faturamento total: R$ 432,10 (12 ms)
   ```
2. Vender todo o estoque de um produto pelo frontend e, em até 1 minuto, ele aparece na tela **Reposição**.
3. Registrar uma entrada do produto: a reposição sai da lista de pendentes (a API resolve).
4. `GET http://localhost:8081/status` mostra a última execução e o resumo calculado.

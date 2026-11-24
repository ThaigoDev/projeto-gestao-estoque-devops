# estoque-api — Backend (Pessoa 2)

API REST do Sistema de Estoque e Vendas.
**Java 21 + Spring Boot 3.5 + MongoDB.**

## Como rodar

### Localmente (precisa de um MongoDB em `localhost:27017`)

```bash
docker run -d --name mongo -p 27017:27017 mongo:7   # se ainda nao tiver Mongo
./mvnw spring-boot:run                                # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. Teste: `GET http://localhost:8080/api/info`.

### Com Docker

```bash
docker build -t SEU_USUARIO/estoque-api:v1 .
docker run -p 8080:8080 -e MONGODB_URI=mongodb://mongo:27017/estoque --network SUA_REDE SEU_USUARIO/estoque-api:v1
```

### Testes

```bash
./mvnw test
```

Os testes de integracao usam um MongoDB embutido (Flapdoodle): nao precisam de Docker. Na primeira execucao ele baixa o MongoDB 7 (~100 MB) do site oficial e guarda em cache.

## Variaveis de ambiente

| Variavel | Padrao | Uso |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/estoque` (no Docker: `mongodb://mongo:27017/estoque`) | Conexao com o banco |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origens do frontend liberadas (separadas por virgula; aceita `*`) |
| `APP_TIMEZONE` | `America/Sao_Paulo` | Fuso para "hoje"/"mes" nos relatorios |
| `APP_VERSION` | `v1` | Aparece em `/api/info` — use `v2` na imagem nova para demonstrar o rolling update |

## Para a Pessoa 4 (Docker / Kubernetes)

- Porta do container: **8080**
- Health checks (Actuator):
  - liveness: `GET /actuator/health/liveness`
  - readiness: `GET /actuator/health/readiness`
- `GET /api/info` devolve `versao` e `instancia` (hostname = nome do Pod). Chamando varias vezes pelo Service da para mostrar o balanceamento entre replicas e a troca v1 → v2.
- A API nao guarda estado em memoria, entao pode ter quantas replicas quiser.

Exemplo de probes no Deployment:

```yaml
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  initialDelaySeconds: 20
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
  initialDelaySeconds: 40
env:
  - name: MONGODB_URI
    value: mongodb://mongo:27017/estoque
```

## Banco de dados (MongoDB, database `estoque`)

As colecoes e os indices sao criados automaticamente na primeira execucao.

| Colecao | Quem escreve | Conteudo |
|---|---|---|
| `produtos` | API | Cadastro + `quantidadeEstoque` |
| `fornecedores` | API | Fornecedores (nome, CNPJ, telefone, e-mail) |
| `entradas` | API | Historico de entradas de mercadoria (com `fornecedorId`) |
| `vendas` | API | Vendas com os itens embutidos (nome e preco do momento da venda) |
| `reposicao` | **Worker** (cria) / API (resolve) | Produtos que zeraram e precisam de reposicao |

Valores em dinheiro sao gravados como **Decimal128** (nao como double, para nao perder centavos).

### Contrato com o Worker (Pessoa 3)

O Worker, a cada minuto:

1. Busca em `produtos` os documentos com `ativo: true` e `quantidadeEstoque: 0` (ou menor).
2. Para cada um, **se ainda nao existir** documento em `reposicao` com `produtoId` igual e `status: "PENDENTE"`, insere:

```json
{
  "produtoId": "<_id do produto, como string>",
  "produtoNome": "Arroz 5kg",
  "quantidadeAtual": 0,
  "status": "PENDENTE",
  "dataRegistro": ISODate("2026-11-18T15:00:00Z")
}
```

Quando chega uma entrada de mercadoria, a API muda as reposicoes `PENDENTE` daquele produto para `RESOLVIDA` e preenche `dataResolucao`. Assim o Worker nao precisa saber das entradas.

> Atencao: o `_id` do produto e um ObjectId no Mongo; em `reposicao.produtoId` gravem como **string** (`produto._id.toString()` / `toHexString()`).

## Endpoints

Base: `http://localhost:8080`. Todos recebem e devolvem JSON. Datas de filtro no formato `AAAA-MM-DD`.

### Produtos

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/produtos` | Lista produtos ativos (ordem alfabetica) |
| GET | `/api/produtos?nome=arroz` | Busca por parte do nome |
| GET | `/api/produtos?categoria=Bebidas` | Filtra por categoria |
| GET | `/api/produtos?estoque=zerado` | Somente sem estoque |
| GET | `/api/produtos?estoque=baixo` | Estoque ≤ estoque minimo |
| GET | `/api/produtos/{id}` | Detalhe |
| POST | `/api/produtos` | Cadastra (201) |
| PUT | `/api/produtos/{id}` | Edita dados cadastrais (**nao altera o estoque**) |
| DELETE | `/api/produtos/{id}` | Desativa (204) — continua no historico de vendas |
| GET | `/api/produtos/{id}/entradas` | Historico de entradas do produto |

Corpo do POST/PUT:

```json
{
  "codigo": "ARZ-5KG",
  "nome": "Arroz 5kg",
  "descricao": "Arroz tipo 1",
  "categoria": "Alimentos",
  "precoVenda": 27.90,
  "precoCusto": 19.50,
  "estoqueMinimo": 5,
  "estoqueInicial": 20
}
```

Obrigatorios: `nome`, `precoVenda`. `estoqueInicial` so vale no POST e gera uma entrada "Estoque inicial".

Resposta (campos `semEstoque` e `estoqueBaixo` sao calculados para facilitar a tela):

```json
{
  "id": "673b...",
  "codigo": "ARZ-5KG",
  "nome": "Arroz 5kg",
  "categoria": "Alimentos",
  "precoVenda": 27.90,
  "precoCusto": 19.50,
  "quantidadeEstoque": 20,
  "estoqueMinimo": 5,
  "ativo": true,
  "semEstoque": false,
  "estoqueBaixo": false,
  "criadoEm": "2026-11-18T10:00:00Z",
  "atualizadoEm": "2026-11-18T10:00:00Z"
}
```

### Entradas de mercadoria

| Metodo | Rota | Descricao |
|---|---|---|
| POST | `/api/entradas` | Registra entrada e soma ao estoque (201) |
| GET | `/api/entradas` | Todas as entradas (mais recentes primeiro) |
| GET | `/api/entradas?produtoId={id}` | Entradas de um produto |

```json
{ "produtoId": "673b...", "quantidade": 50, "custoUnitario": 19.00, "fornecedor": "Distribuidora X", "observacao": "NF 1234" }
```

Obrigatorios: `produtoId`, `quantidade` (≥ 1).

O fornecedor e opcional e pode vir de duas formas:
- `fornecedorId`: um fornecedor cadastrado em `/api/fornecedores`;
- `fornecedor` (nome): a API procura pelo nome (sem diferenciar maiusculas) e, se nao existir, **cadastra automaticamente** na colecao `fornecedores`.

A resposta traz `fornecedorId` e `fornecedor` (nome).

### Fornecedores

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/fornecedores` | Lista fornecedores ativos (`?nome=` filtra) |
| GET | `/api/fornecedores/{id}` | Detalhe |
| POST | `/api/fornecedores` | Cadastra (201). Nome repetido → 409 |
| PUT | `/api/fornecedores/{id}` | Edita |
| DELETE | `/api/fornecedores/{id}` | Desativa (204) |

```json
{ "nome": "Distribuidora Central", "cnpj": "34.567.890/0001-12", "telefone": "(11) 3333-3003", "email": "contato@distcentral.example" }
```

Obrigatorio: `nome`.

### Vendas

| Metodo | Rota | Descricao |
|---|---|---|
| POST | `/api/vendas` | Registra venda e baixa o estoque (201) |
| GET | `/api/vendas` | Historico completo (mais recentes primeiro) |
| GET | `/api/vendas?inicio=2026-11-01&fim=2026-11-30` | Historico no periodo |
| GET | `/api/vendas/{id}` | Detalhe |

```json
{
  "itens": [
    { "produtoId": "673b...", "quantidade": 2 },
    { "produtoId": "673c...", "quantidade": 1 }
  ],
  "formaPagamento": "PIX",
  "cliente": "Maria"
}
```

`formaPagamento`: `DINHEIRO`, `PIX`, `CARTAO_DEBITO`, `CARTAO_CREDITO`.
O **preco vem do cadastro do produto**, nunca do frontend. A resposta traz `itens` (com `precoUnitario` e `subtotal`), `valorTotal`, `quantidadeItens` e `dataVenda`.

Regras:
- Se qualquer item nao tiver estoque suficiente, **a venda inteira e recusada** (HTTP 422) e nada e baixado.
- A baixa e atomica no MongoDB: duas vendas simultaneas nao conseguem vender a mesma ultima unidade.
- Itens repetidos do mesmo produto sao somados.

### Dados de exemplo (requisito R3)

| Metodo | Rota | Descricao |
|---|---|---|
| POST | `/api/dados-exemplo` | Carrega dados ficticios de uma padaria/mercadinho. Recusa (422) se ja houver produtos |
| POST | `/api/dados-exemplo?limpar=true` | **Apaga todas as colecoes** e carrega de novo |

Carrega 3 fornecedores, 12 produtos (com entrada de estoque de cada um) e 20 vendas espalhadas pelos ultimos 14 dias.
Dois produtos (acucar e suco) terminam com estoque zero, entao em ate 1 minuto o Worker registra a reposicao deles.

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/dados-exemplo?limpar=true"
```
```bash
curl -X POST "http://localhost:8080/api/dados-exemplo?limpar=true"
```

No Kubernetes, como a API nao e exposta fora do cluster, use o frontend (o Nginx repassa `/api/`) ou um port-forward:
`kubectl -n estoque port-forward svc/api 9090:8080` e chame `http://localhost:9090/api/dados-exemplo?limpar=true`.

### Relatorios e dashboard

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/dashboard` | Totais para a tela inicial |
| GET | `/api/relatorios/faturamento?inicio=&fim=` | Faturamento e quantidade de vendas (padrao: ultimos 30 dias) |
| GET | `/api/relatorios/produtos-mais-vendidos?inicio=&fim=&limite=10` | Ranking de produtos |
| GET | `/api/reposicoes` | Reposicoes pendentes (gravadas pelo Worker). `?status=RESOLVIDA` ou `?status=TODAS` |
| GET | `/api/info` | Versao e instancia (Pod) |

`/api/dashboard`:

```json
{
  "totalProdutos": 12, "produtosSemEstoque": 2, "produtosEstoqueBaixo": 4, "reposicoesPendentes": 2,
  "faturamentoHoje": 245.70, "vendasHoje": 6, "faturamentoMes": 5320.10, "vendasMes": 87
}
```

`/api/relatorios/faturamento` (o `porDia` traz todos os dias do periodo, inclusive com zero — pronto para grafico):

```json
{
  "inicio": "2026-11-01", "fim": "2026-11-18",
  "faturamentoTotal": 5320.10, "quantidadeVendas": 87, "quantidadeItensVendidos": 240, "ticketMedio": 61.15,
  "porFormaPagamento": { "DINHEIRO": 800.00, "PIX": 3020.10, "CARTAO_CREDITO": 1500.00 },
  "porDia": [ { "data": "2026-11-01", "faturamento": 310.00, "quantidadeVendas": 5 } ]
}
```

### Erros

Todas as respostas de erro seguem o mesmo formato:

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Estoque insuficiente para 'Arroz 5kg': disponivel 1, solicitado 3",
  "timestamp": "2026-11-18T15:00:00Z",
  "detalhes": { "produtoId": "673b...", "disponivel": 1, "solicitado": 3 }
}
```

| Status | Quando |
|---|---|
| 400 | Campos invalidos (`detalhes` traz campo → mensagem) ou JSON mal formado |
| 404 | Produto/venda nao encontrado |
| 409 | Codigo de produto ja cadastrado |
| 422 | Regra de negocio (ex.: estoque insuficiente, periodo invalido) |

## Estrutura

```
src/main/java/br/com/estoque/api
├── config/        CORS e relogio (fuso)
├── controller/    Endpoints REST
├── dto/           Corpos de requisicao e respostas de relatorio
├── exception/     Erros e handler global
├── model/         Documentos do MongoDB
├── repository/    Spring Data MongoDB
└── service/       Regras de negocio (EstoqueService e o unico que altera estoque)
```

O arquivo [`requests.http`](requests.http) tem exemplos prontos de todas as chamadas (abre no IntelliJ ou na extensao REST Client do VS Code).

# DevOps – Docker, Compose e Kubernetes (Pessoa 4)

Troque `SEU_USUARIO` pelo usuário do Docker Hub em todos os comandos.

## 1. Docker Compose (local)

```bash
cp .env.example .env                       # ajuste DOCKERHUB_USER
docker compose up -d --build               # mongo + api + frontend + worker
```

- Frontend: http://localhost:3000 · API: http://localhost:8080/api/info
- O MongoDB usa o volume nomeado `mongo-data`. Teste de persistência:
  `docker compose down` (sem `-v`) e `docker compose up -d` → os dados continuam.
  `docker compose down -v` apaga o volume.

## 2. Publicar as imagens v1 no Docker Hub

```bash
docker login
docker build -t SEU_USUARIO/estoque-api:v1      ./api
docker build -t SEU_USUARIO/estoque-frontend:v1 ./frontend
docker build -t SEU_USUARIO/estoque-worker:v1   ./worker
docker push SEU_USUARIO/estoque-api:v1
docker push SEU_USUARIO/estoque-frontend:v1
docker push SEU_USUARIO/estoque-worker:v1
```

## 3. Cluster Kubernetes com 3 nós (kind)

```bash
kind create cluster --name estoque --config k8s/kind-cluster.yaml
kubectl get nodes                           # 1 control-plane + 2 workers
```

Edite `k8s/kustomization.yaml` (usuário do Docker Hub) e aplique:

```bash
kubectl apply -k k8s
kubectl -n estoque get pods -o wide -w      # note em quais nós os Pods caem
```

Acesse: http://localhost:8081 (NodePort 30080 mapeado pelo kind).
Estrutura: `mongo` (StatefulSet + PVC 1Gi), `api` (3 réplicas, ClusterIP), `frontend` (3 réplicas, NodePort), `worker` (1 réplica).

## 4. Demonstrações

**Exclusão de Pods (auto-recuperação):**
```bash
kubectl -n estoque delete pod -l app=api    # apaga os Pods da API
kubectl -n estoque get pods -w              # o Deployment recria sozinho
```

**Persistência do Mongo:** cadastre um produto, rode `kubectl -n estoque delete pod mongo-0`, espere voltar e confira que o produto continua lá (o PVC permanece).

**Balanceamento entre réplicas:** a API devolve o nome do Pod:
```bash
kubectl -n estoque port-forward svc/api 9090:8080
# em outro terminal, repita:  curl http://localhost:9090/api/info
```

**Escalabilidade:**
```bash
kubectl -n estoque scale deployment/api --replicas=5
kubectl -n estoque get pods -l app=api
kubectl -n estoque scale deployment/api --replicas=3
```

**Atualização v1 → v2 (rolling update, sem downtime):**
```bash
docker build --build-arg APP_VERSION=v2 -t SEU_USUARIO/estoque-api:v2      ./api
docker build --build-arg APP_VERSION=v2 -t SEU_USUARIO/estoque-frontend:v2 ./frontend
docker push SEU_USUARIO/estoque-api:v2
docker push SEU_USUARIO/estoque-frontend:v2

kubectl -n estoque set image deployment/api      api=SEU_USUARIO/estoque-api:v2
kubectl -n estoque set image deployment/frontend frontend=SEU_USUARIO/estoque-frontend:v2
kubectl -n estoque rollout status deployment/api
curl http://localhost:9090/api/info          # "versao": "v2"

kubectl -n estoque rollout undo deployment/api   # volta para v1, se precisar
```

## Observações

- `api/Dockerfile` ganhou `ARG APP_VERSION` para permitir construir a imagem v2 sem mudar código.
- O worker roda com 1 réplica (mais de uma duplicaria a checagem a cada minuto).
- Sem Docker Hub (teste offline): `kind load docker-image SEU_USUARIO/estoque-api:v1 --name estoque`.

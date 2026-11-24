# DevOps – Docker, Compose e Kubernetes

Os comandos estao em PowerShell (Windows). No Linux/macOS sao os mesmos, trocando
`Invoke-RestMethod` por `curl` e `.ps1` por `.sh`.

Troque `SEU_USUARIO` pelo usuario do Docker Hub. As imagens do grupo estao em `lucashc1011`.

---

## 1. Docker Compose (D2, D3, D5)

So precisa de Docker e Docker Compose instalados.

```powershell
docker compose up -d --build
Invoke-RestMethod -Method Post "http://localhost:3000/api/dados-exemplo?limpar=true"   # dados ficticios (R3)
```

- Sistema: http://localhost:3000
- API: http://localhost:8080/api/info
- O MongoDB **nao publica porta** no computador: a API e o Worker chegam nele pelo nome do servico (`mongodb://mongo:27017/estoque`) na rede `estoque` do Compose.

### D3 – Dados permanecem apos recriar o container do banco

```powershell
docker compose exec mongo mongosh estoque --quiet --eval "db.produtos.countDocuments()"   # anote o numero
docker compose rm -sf mongo          # para e APAGA o container do Mongo
docker compose up -d mongo           # cria um container novo
docker compose exec mongo mongosh estoque --quiet --eval "db.produtos.countDocuments()"   # mesmo numero
```

Os dados ficam no volume nomeado `mongo-data`, nao no container.
(`docker compose down -v` apaga o volume e, ai sim, os dados.)

---

## 2. Publicar as imagens v1 no Docker Hub (D4)

```powershell
docker login
.\scripts\publicar-imagens.ps1 -Usuario SEU_USUARIO -Versao v1
```

O script gera e envia `SEU_USUARIO/estoque-api:v1`, `estoque-frontend:v1` e `estoque-worker:v1`.

---

## 3. Cluster Kubernetes (K1 – K4)

### K1 – Cluster com 1 no de controle e 2 nos de trabalho (kind)

Instale o kind (uma vez): `winget install Kubernetes.kind`

```powershell
kind create cluster --name estoque --config k8s/kind-cluster.yaml
kubectl get nodes          # estoque-control-plane, estoque-worker, estoque-worker2
```

> Erro `Bind for 0.0.0.0:8081 failed: port is already allocated`: outro programa usa a porta 8081.
> Troque `hostPort: 8081` em `k8s/kind-cluster.yaml` por outra porta livre (ex.: 8085), rode
> `kind delete cluster --name estoque` e crie de novo. O sistema passa a ficar em `http://localhost:<porta>`.

### K2/K3/K4 – Subir os servicos

Se o usuario do Docker Hub nao for `lucashc1011`, troque em `k8s/kustomization.yaml` (`newName`).

```powershell
kubectl apply -k k8s
kubectl -n estoque get pods -o wide -w     # Ctrl+C quando todos estiverem Running/Ready
```

| Servico | Objeto | Replicas | Service | Acesso |
|---|---|---|---|---|
| frontend | Deployment | 1 | NodePort 30080 | **navegador: http://localhost:8081** |
| api | Deployment | 2 | ClusterIP | so dentro do cluster (o Nginx do frontend repassa `/api/`) |
| mongo | Deployment + PVC 1Gi | 1 | ClusterIP | so dentro do cluster |
| worker | Deployment | 1 | ClusterIP | so dentro do cluster |

Carregar os dados de exemplo pelo frontend:

```powershell
Invoke-RestMethod -Method Post "http://localhost:8081/api/dados-exemplo?limpar=true"
```

**Por que o Worker tem exatamente 1 replica?** Ele nao atende requisicoes: e uma tarefa
agendada que roda a cada minuto. Com 2 replicas, a mesma verificacao rodaria duas vezes por
minuto, em paralelo, e as duas poderiam registrar a mesma reposicao em duplicidade. Por isso
ele usa 1 replica e estrategia `Recreate` (na atualizacao, o Pod antigo para antes do novo subir).

**Por que a API e o Worker tem um `initContainer` (`espera-mongo`)?** No primeiro deploy os Pods
sobem ao mesmo tempo; sem ele, a API iniciaria antes do Mongo, falharia apos 30 s e seria reiniciada.
O initContainer so espera `mongo:27017` aceitar conexoes, entao todos sobem com `RESTARTS 0`.

**Por que o Mongo usa `Recreate`?** O volume `ReadWriteOnce` so pode ser usado por um Pod de
cada vez, entao o Pod antigo precisa sair antes do novo montar o volume.

---

## 4. Demonstracoes

Deixe um terminal mostrando os Pods o tempo todo:

```powershell
kubectl -n estoque get pods -o wide -w
```

E, em outro, um "monitor" que chama a API pelo frontend a cada segundo (mostra versao e qual Pod respondeu):

```powershell
while ($true) { try { $i = Invoke-RestMethod http://localhost:8081/api/info -TimeoutSec 2; "$(Get-Date -f HH:mm:ss)  $($i.versao)  $($i.instancia)" } catch { "$(Get-Date -f HH:mm:ss)  FALHOU" }; Start-Sleep 1 }
```

### K5 – Falha da API

```powershell
kubectl -n estoque get pods -l app=api
kubectl -n estoque delete pod <nome-de-um-pod-da-api>
kubectl -n estoque get pods -l app=api      # um Pod novo aparece no lugar
```

O monitor continua respondendo: a outra replica atende enquanto o Pod novo sobe.

### K6 – Persistencia do banco

```powershell
kubectl -n estoque exec deploy/mongo -- mongosh estoque --quiet --eval "db.produtos.countDocuments()"
kubectl -n estoque delete pod -l app=mongo
kubectl -n estoque get pods -l app=mongo -w          # espere o novo ficar 1/1 Running
kubectl -n estoque exec deploy/mongo -- mongosh estoque --quiet --eval "db.produtos.countDocuments()"
```

O numero e o mesmo e os produtos continuam na tela: os dados estao no PVC `mongo-data`
(`kubectl -n estoque get pvc`), que nao e apagado junto com o Pod.

### K7 – Escala da API (2 → 3 → 2)

```powershell
kubectl -n estoque scale deployment/api --replicas=3
kubectl -n estoque get pods -l app=api          # 3 Pods
kubectl -n estoque get deployment api           # READY 3/3
kubectl -n estoque scale deployment/api --replicas=2
kubectl -n estoque get pods -l app=api          # volta para 2
```

### K8 – Atualizacao de versao (v1 → v2 → v1)

A "mudanca visivel": o rodape do menu do frontend mostra a versao do frontend e da API,
e `/api/info` devolve `"versao"`. Gerar a v2 nao exige mudar codigo, so o `--build-arg`:

```powershell
.\scripts\publicar-imagens.ps1 -Usuario SEU_USUARIO -Versao v2 -Servicos api,frontend
```

Atualizar (rolling update: com `maxUnavailable: 0` sempre ha um Pod pronto atendendo):

```powershell
kubectl -n estoque set image deployment/api api=SEU_USUARIO/estoque-api:v2
kubectl -n estoque set image deployment/frontend frontend=SEU_USUARIO/estoque-frontend:v2
kubectl -n estoque rollout status deployment/api
kubectl -n estoque rollout status deployment/frontend
```

O monitor passa de `v1` para `v2` sem mostrar `FALHOU`, e o rodape do site (Ctrl+F5) mostra v2.

Voltar para a v1:

```powershell
kubectl -n estoque rollout undo deployment/api
kubectl -n estoque rollout undo deployment/frontend
kubectl -n estoque rollout history deployment/api
```

---

## 5. Sem Docker Hub (teste local)

Para testar o cluster sem publicar, carregue as imagens locais nos nos do kind:

```powershell
docker compose build
docker tag local/estoque-api:v1      lucashc1011/estoque-api:v1
docker tag local/estoque-frontend:v1 lucashc1011/estoque-frontend:v1
docker tag local/estoque-worker:v1   lucashc1011/estoque-worker:v1
kind load docker-image lucashc1011/estoque-api:v1 lucashc1011/estoque-frontend:v1 lucashc1011/estoque-worker:v1 --name estoque
```

## 6. Limpeza

```powershell
kind delete cluster --name estoque
docker compose down          # mantem os dados (use -v para apagar o volume)
```

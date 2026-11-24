#!/usr/bin/env sh
# Gera e publica as imagens no Docker Hub (requisito D4 e imagem v2 do K8).
#
#   docker login
#   ./scripts/publicar-imagens.sh lucashc1011 v1
#   ./scripts/publicar-imagens.sh lucashc1011 v2 api frontend
set -e

USUARIO="${1:?Informe o usuario do Docker Hub}"
VERSAO="${2:-v1}"
shift 2 2>/dev/null || shift $#
SERVICOS="${*:-api frontend worker}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"

for servico in $SERVICOS; do
  imagem="$USUARIO/estoque-$servico:$VERSAO"
  echo "==> build $imagem"
  docker build --build-arg "APP_VERSION=$VERSAO" -t "$imagem" "$RAIZ/$servico"
  echo "==> push $imagem"
  docker push "$imagem"
done

echo "Pronto: $SERVICOS na versao $VERSAO"

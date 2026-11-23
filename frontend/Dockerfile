# ---- Etapa 1: build da aplicacao React ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Versao exibida no rodape do menu (usar v2 na demonstracao de atualizacao).
ARG APP_VERSION=v1
ENV VITE_APP_VERSION=$APP_VERSION
RUN npm run build

# ---- Etapa 2: Nginx servindo os arquivos estaticos ----
FROM nginx:1.27-alpine

# A imagem oficial do Nginx aplica envsubst nos arquivos de /etc/nginx/templates
# na inicializacao, trocando ${API_URL} pelo valor da variavel de ambiente.
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Endereco da API dentro da rede do Docker Compose / Service do Kubernetes.
ENV API_URL=http://api:8080

EXPOSE 80

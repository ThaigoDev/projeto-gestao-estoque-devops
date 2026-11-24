# Gera e publica as imagens no Docker Hub (requisito D4 e imagem v2 do K8).
#
#   docker login
#   .\scripts\publicar-imagens.ps1 -Usuario lucashc1011 -Versao v1
#   .\scripts\publicar-imagens.ps1 -Usuario lucashc1011 -Versao v2 -Servicos api,frontend
#
# A versao vai como --build-arg APP_VERSION e aparece em /api/info e no rodape do frontend.
param(
    [Parameter(Mandatory = $true)] [string] $Usuario,
    [string] $Versao = "v1",
    [string[]] $Servicos = @("api", "frontend", "worker"),
    [switch] $SemPush
)

# Sem $ErrorActionPreference = "Stop": no PowerShell 5 o progresso do docker (stderr) viraria erro.
# Cada comando e conferido pelo $LASTEXITCODE.
$raiz = Split-Path -Parent $PSScriptRoot

foreach ($servico in $Servicos) {
    $imagem = "$Usuario/estoque-${servico}:$Versao"
    Write-Host "==> build $imagem" -ForegroundColor Cyan
    docker build --build-arg "APP_VERSION=$Versao" -t $imagem (Join-Path $raiz $servico)
    if ($LASTEXITCODE -ne 0) { throw "Falha no build de $servico" }

    if (-not $SemPush) {
        Write-Host "==> push $imagem" -ForegroundColor Cyan
        docker push $imagem
        if ($LASTEXITCODE -ne 0) { throw "Falha no push de $imagem (fez docker login?)" }
    }
}

Write-Host "Pronto: $($Servicos -join ', ') na versao $Versao" -ForegroundColor Green

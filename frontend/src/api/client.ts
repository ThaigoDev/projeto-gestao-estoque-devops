import type {
  Dashboard,
  Entrada,
  EntradaRequest,
  Faturamento,
  InfoApi,
  Produto,
  ProdutoMaisVendido,
  ProdutoRequest,
  Reposicao,
  Venda,
  VendaRequest,
} from './types';

/**
 * Por padrao as chamadas usam caminho relativo (/api/...):
 * - em desenvolvimento o Vite repassa para a API (vite.config.ts);
 * - no Docker/Kubernetes o Nginx do container repassa (nginx/default.conf.template).
 * VITE_API_URL so e necessario se a API estiver em outro endereco sem proxy.
 */
const BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

/** Erro no formato padrao da API: { status, erro, mensagem, detalhes }. */
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public detalhes?: Record<string, unknown>,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type Params = Record<string, string | number | undefined>;

function montarUrl(caminho: string, params?: Params): string {
  const query = new URLSearchParams();
  Object.entries(params ?? {}).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== '') query.set(chave, String(valor));
  });
  const qs = query.toString();
  return `${BASE_URL}${caminho}${qs ? `?${qs}` : ''}`;
}

async function requisicao<T>(metodo: string, caminho: string, corpo?: unknown, params?: Params): Promise<T> {
  let resposta: Response;
  try {
    resposta = await fetch(montarUrl(caminho, params), {
      method: metodo,
      headers: corpo !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new ApiError('Não foi possível conectar à API. Verifique se ela está rodando.', 0);
  }

  const texto = await resposta.text();
  let dados: unknown;
  try {
    dados = texto ? JSON.parse(texto) : undefined;
  } catch {
    dados = undefined;
  }

  if (!resposta.ok) {
    const erro = dados as { mensagem?: string; detalhes?: Record<string, unknown> } | undefined;
    // 500/502/503/504 sem corpo da API = o proxy (Vite/Nginx) nao conseguiu falar com ela.
    if (!erro?.mensagem && [500, 502, 503, 504].includes(resposta.status)) {
      throw new ApiError('Não foi possível conectar à API. Verifique se ela está rodando.', resposta.status);
    }
    throw new ApiError(erro?.mensagem ?? `Erro ${resposta.status} ao chamar a API`, resposta.status, erro?.detalhes);
  }
  return dados as T;
}

/** Mensagem amigavel para exibir na tela, incluindo erros de validacao por campo. */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    if (erro.status === 400 && erro.detalhes) {
      const campos = Object.entries(erro.detalhes)
        .map(([campo, msg]) => `${campo}: ${String(msg)}`)
        .join('; ');
      return `${erro.message} (${campos})`;
    }
    return erro.message;
  }
  return erro instanceof Error ? erro.message : 'Erro inesperado';
}

export const api = {
  produtos: {
    listar: (filtros?: { nome?: string; categoria?: string; estoque?: 'zerado' | 'baixo' }) =>
      requisicao<Produto[]>('GET', '/api/produtos', undefined, filtros),
    buscar: (id: string) => requisicao<Produto>('GET', `/api/produtos/${id}`),
    criar: (dados: ProdutoRequest) => requisicao<Produto>('POST', '/api/produtos', dados),
    atualizar: (id: string, dados: ProdutoRequest) => requisicao<Produto>('PUT', `/api/produtos/${id}`, dados),
    desativar: (id: string) => requisicao<void>('DELETE', `/api/produtos/${id}`),
  },
  entradas: {
    listar: (produtoId?: string) => requisicao<Entrada[]>('GET', '/api/entradas', undefined, { produtoId }),
    registrar: (dados: EntradaRequest) => requisicao<Entrada>('POST', '/api/entradas', dados),
  },
  vendas: {
    listar: (inicio?: string, fim?: string) => requisicao<Venda[]>('GET', '/api/vendas', undefined, { inicio, fim }),
    registrar: (dados: VendaRequest) => requisicao<Venda>('POST', '/api/vendas', dados),
  },
  dashboard: () => requisicao<Dashboard>('GET', '/api/dashboard'),
  relatorios: {
    faturamento: (inicio?: string, fim?: string) =>
      requisicao<Faturamento>('GET', '/api/relatorios/faturamento', undefined, { inicio, fim }),
    maisVendidos: (inicio?: string, fim?: string, limite = 10) =>
      requisicao<ProdutoMaisVendido[]>('GET', '/api/relatorios/produtos-mais-vendidos', undefined, {
        inicio,
        fim,
        limite,
      }),
  },
  reposicoes: (status: 'PENDENTE' | 'RESOLVIDA' | 'TODAS' = 'PENDENTE') =>
    requisicao<Reposicao[]>('GET', '/api/reposicoes', undefined, { status }),
  info: () => requisicao<InfoApi>('GET', '/api/info'),
};

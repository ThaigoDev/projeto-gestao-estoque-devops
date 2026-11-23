// Tipos que espelham as respostas da API (ver api/README.md).

export interface Produto {
  id: string;
  codigo?: string;
  nome: string;
  descricao?: string;
  categoria?: string;
  precoVenda: number;
  precoCusto?: number;
  quantidadeEstoque: number;
  estoqueMinimo: number;
  ativo: boolean;
  semEstoque: boolean;
  estoqueBaixo: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface ProdutoRequest {
  codigo?: string;
  nome: string;
  descricao?: string;
  categoria?: string;
  precoVenda: number;
  precoCusto?: number;
  estoqueMinimo?: number;
  /** Usado so no cadastro (POST). */
  estoqueInicial?: number;
}

export interface Entrada {
  id: string;
  produtoId: string;
  produtoNome: string;
  quantidade: number;
  custoUnitario?: number;
  fornecedor?: string;
  observacao?: string;
  estoqueResultante: number;
  dataEntrada: string;
}

export interface EntradaRequest {
  produtoId: string;
  quantidade: number;
  custoUnitario?: number;
  fornecedor?: string;
  observacao?: string;
}

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO';

export interface ItemVenda {
  produtoId: string;
  produtoNome: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
}

export interface Venda {
  id: string;
  itens: ItemVenda[];
  valorTotal: number;
  quantidadeItens: number;
  formaPagamento: FormaPagamento;
  cliente?: string;
  dataVenda: string;
}

export interface VendaRequest {
  itens: { produtoId: string; quantidade: number }[];
  formaPagamento: FormaPagamento;
  cliente?: string;
}

export interface Dashboard {
  totalProdutos: number;
  produtosSemEstoque: number;
  produtosEstoqueBaixo: number;
  reposicoesPendentes: number;
  faturamentoHoje: number;
  vendasHoje: number;
  faturamentoMes: number;
  vendasMes: number;
}

export interface FaturamentoDiario {
  data: string;
  faturamento: number;
  quantidadeVendas: number;
}

export interface Faturamento {
  inicio: string;
  fim: string;
  faturamentoTotal: number;
  quantidadeVendas: number;
  quantidadeItensVendidos: number;
  ticketMedio: number;
  porFormaPagamento: Partial<Record<FormaPagamento, number>>;
  porDia: FaturamentoDiario[];
}

export interface ProdutoMaisVendido {
  produtoId: string;
  produtoNome: string;
  quantidadeVendida: number;
  faturamento: number;
}

export type StatusReposicao = 'PENDENTE' | 'RESOLVIDA';

export interface Reposicao {
  id: string;
  produtoId: string;
  produtoNome: string;
  quantidadeAtual: number;
  status: StatusReposicao;
  dataRegistro: string;
  dataResolucao?: string;
}

export interface InfoApi {
  servico: string;
  versao: string;
  instancia: string;
}

import type { FormaPagamento } from '../api/types';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const numero = new Intl.NumberFormat('pt-BR');
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const formatarMoeda = (valor: number | undefined | null) => moeda.format(Number(valor ?? 0));

export const formatarNumero = (valor: number | undefined | null) => numero.format(Number(valor ?? 0));

export const formatarDataHora = (iso: string | undefined) => (iso ? dataHora.format(new Date(iso)) : '-');

/** "2026-11-18" -> "18/11" (datas sem horario vindas dos relatorios). */
export const formatarDiaCurto = (data: string) => {
  const [, mes, dia] = data.split('-');
  return `${dia}/${mes}`;
};

/** "2026-11-18" -> "18/11/2026". */
export const formatarData = (data: string) => {
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
};

/** Data local no formato AAAA-MM-DD, usado nos filtros da API. */
export function dataISO(data: Date = new Date()): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function diasAtras(dias: number): string {
  const data = new Date();
  data.setDate(data.getDate() - dias);
  return dataISO(data);
}

export function inicioDoMes(): string {
  const hoje = new Date();
  return dataISO(new Date(hoje.getFullYear(), hoje.getMonth(), 1));
}

export const NOMES_PAGAMENTO: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  CARTAO_DEBITO: 'Cartão de débito',
  CARTAO_CREDITO: 'Cartão de crédito',
};

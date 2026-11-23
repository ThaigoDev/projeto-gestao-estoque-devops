import { useState } from 'react';
import { api } from '../api/client';
import type { FormaPagamento } from '../api/types';
import { GraficoBarras, GraficoColunas } from '../components/Graficos';
import { CabecalhoPagina, CartaoIndicador, Carregando, MensagemErro, Vazio } from '../components/Ui';
import { IconeCaixa, IconeDinheiro, IconeRecibo, IconeTicket } from '../components/Icones';
import {
  NOMES_PAGAMENTO,
  dataISO,
  diasAtras,
  formatarData,
  formatarDiaCurto,
  formatarMoeda,
  formatarNumero,
  inicioDoMes,
} from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

const hoje = () => dataISO(new Date());

const PERIODOS = [
  { rotulo: 'Últimos 7 dias', inicio: () => diasAtras(6) },
  { rotulo: 'Últimos 30 dias', inicio: () => diasAtras(29) },
  { rotulo: 'Este mês', inicio: inicioDoMes },
];

export function RelatoriosPage() {
  const [inicio, setInicio] = useState(diasAtras(29));
  const [fim, setFim] = useState(hoje());

  const { dados, erro, carregando, recarregar } = useCarregar(
    async () => {
      const [faturamento, maisVendidos] = await Promise.all([
        api.relatorios.faturamento(inicio, fim),
        api.relatorios.maisVendidos(inicio, fim, 10),
      ]);
      return { faturamento, maisVendidos };
    },
    [inicio, fim],
  );

  function escolherPeriodo(i: number) {
    setInicio(PERIODOS[i].inicio());
    setFim(hoje());
  }

  const periodoAtivo = PERIODOS.findIndex((p) => p.inicio() === inicio && fim === hoje());

  return (
    <>
      <CabecalhoPagina titulo="Relatórios" descricao="Faturamento, quantidade de vendas e produtos mais vendidos" />

      <section className="painel">
        <div className="filtros">
          <div className="segmentado" role="group" aria-label="Período">
            {PERIODOS.map((p, i) => (
              <button key={p.rotulo} type="button" className={periodoAtivo === i ? 'ativo' : ''} onClick={() => escolherPeriodo(i)}>
                {p.rotulo}
              </button>
            ))}
          </div>
          <label className="campo campo-inline">
            De
            <input type="date" value={inicio} max={fim} onChange={(e) => setInicio(e.target.value)} />
          </label>
          <label className="campo campo-inline">
            Até
            <input type="date" value={fim} min={inicio} onChange={(e) => setFim(e.target.value)} />
          </label>
        </div>
      </section>

      {erro && <MensagemErro texto={erro} aoTentarDeNovo={recarregar} />}
      {!dados && carregando && <Carregando />}

      {dados && (
        <>
          <section className="grade-indicadores">
            <CartaoIndicador
              rotulo="Faturamento"
              valor={formatarMoeda(dados.faturamento.faturamentoTotal)}
              detalhe={`${formatarData(dados.faturamento.inicio)} a ${formatarData(dados.faturamento.fim)}`}
              destaque="verde"
              icone={<IconeDinheiro />}
            />
            <CartaoIndicador
              rotulo="Quantidade de vendas"
              valor={formatarNumero(dados.faturamento.quantidadeVendas)}
              destaque="azul"
              icone={<IconeRecibo />}
            />
            <CartaoIndicador
              rotulo="Itens vendidos"
              valor={formatarNumero(dados.faturamento.quantidadeItensVendidos)}
              icone={<IconeCaixa />}
            />
            <CartaoIndicador
              rotulo="Ticket médio"
              valor={formatarMoeda(dados.faturamento.ticketMedio)}
              icone={<IconeTicket />}
            />
          </section>

          <section className="painel">
            <h2>Faturamento por dia</h2>
            <GraficoColunas
              formatar={formatarMoeda}
              pontos={dados.faturamento.porDia.map((d) => ({
                rotulo: formatarDiaCurto(d.data),
                valor: d.faturamento,
                dica: `${formatarData(d.data)}: ${formatarMoeda(d.faturamento)} (${d.quantidadeVendas} venda(s))`,
              }))}
            />
          </section>

          <div className="grade-2">
            <section className="painel">
              <h2>Por forma de pagamento</h2>
              {dados.faturamento.quantidadeVendas === 0 ? (
                <Vazio texto="Sem vendas no período." />
              ) : (
                <GraficoBarras
                  formatar={formatarMoeda}
                  pontos={(Object.keys(NOMES_PAGAMENTO) as FormaPagamento[]).map((forma) => ({
                    rotulo: NOMES_PAGAMENTO[forma],
                    valor: dados.faturamento.porFormaPagamento[forma] ?? 0,
                  }))}
                />
              )}
            </section>

            <section className="painel">
              <h2>Produtos mais vendidos</h2>
              {dados.maisVendidos.length === 0 ? (
                <Vazio texto="Sem vendas no período." />
              ) : (
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Produto</th>
                      <th className="num">Qtd.</th>
                      <th className="num">Faturamento</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.maisVendidos.map((p, i) => (
                      <tr key={p.produtoId}>
                        <td className="texto-suave">{i + 1}</td>
                        <td>{p.produtoNome}</td>
                        <td className="num">{formatarNumero(p.quantidadeVendida)}</td>
                        <td className="num">{formatarMoeda(p.faturamento)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </div>
        </>
      )}
    </>
  );
}

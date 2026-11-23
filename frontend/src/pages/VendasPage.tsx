import { Fragment, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { CabecalhoPagina, CartaoIndicador, Carregando, MensagemErro, Vazio } from '../components/Ui';
import { IconeCaixa, IconeDinheiro, IconeRecibo } from '../components/Icones';
import { NOMES_PAGAMENTO, dataISO, diasAtras, formatarDataHora, formatarMoeda, formatarNumero } from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

export function VendasPage() {
  const [inicio, setInicio] = useState(diasAtras(29));
  const [fim, setFim] = useState(dataISO(new Date()));
  const [aberta, setAberta] = useState<string>();

  const { dados: vendas, erro, carregando, recarregar } = useCarregar(() => api.vendas.listar(inicio, fim), [inicio, fim]);

  const faturamento = (vendas ?? []).reduce((s, v) => s + v.valorTotal, 0);
  const itens = (vendas ?? []).reduce((s, v) => s + v.quantidadeItens, 0);

  return (
    <>
      <CabecalhoPagina
        titulo="Histórico de vendas"
        descricao="Consulte as vendas realizadas por período"
        acoes={
          <Link to="/vendas/nova" className="btn">
            Nova venda
          </Link>
        }
      />

      <section className="painel">
        <div className="filtros">
          <label className="campo campo-inline">
            De
            <input type="date" value={inicio} max={fim} onChange={(e) => setInicio(e.target.value)} />
          </label>
          <label className="campo campo-inline">
            Até
            <input type="date" value={fim} min={inicio} onChange={(e) => setFim(e.target.value)} />
          </label>
          <button type="button" className="btn btn-secundario" onClick={recarregar}>
            Atualizar
          </button>
        </div>

        {vendas && (
          <div className="grade-indicadores">
            <CartaoIndicador rotulo="Vendas no período" valor={formatarNumero(vendas.length)} destaque="azul" icone={<IconeRecibo />} />
            <CartaoIndicador rotulo="Faturamento" valor={formatarMoeda(faturamento)} destaque="verde" icone={<IconeDinheiro />} />
            <CartaoIndicador rotulo="Itens vendidos" valor={formatarNumero(itens)} icone={<IconeCaixa />} />
          </div>
        )}

        {erro && <MensagemErro texto={erro} aoTentarDeNovo={recarregar} />}
        {!vendas && carregando && <Carregando />}

        {vendas &&
          (vendas.length === 0 ? (
            <Vazio texto="Nenhuma venda neste período." />
          ) : (
            <div className="tabela-rolagem">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Cliente</th>
                    <th>Pagamento</th>
                    <th className="num">Itens</th>
                    <th className="num">Total</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {vendas.map((v) => (
                    <Fragment key={v.id}>
                      <tr className="linha-clicavel" onClick={() => setAberta(aberta === v.id ? undefined : v.id)}>
                        <td>{formatarDataHora(v.dataVenda)}</td>
                        <td>{v.cliente ?? <span className="texto-suave">-</span>}</td>
                        <td>{NOMES_PAGAMENTO[v.formaPagamento]}</td>
                        <td className="num">{v.quantidadeItens}</td>
                        <td className="num">
                          <strong>{formatarMoeda(v.valorTotal)}</strong>
                        </td>
                        <td className="acoes-tabela texto-suave">{aberta === v.id ? '▲' : '▼'}</td>
                      </tr>
                      {aberta === v.id && (
                        <tr className="linha-detalhe">
                          <td colSpan={6}>
                            <table className="tabela tabela-interna">
                              <thead>
                                <tr>
                                  <th>Produto</th>
                                  <th className="num">Qtd.</th>
                                  <th className="num">Preço un.</th>
                                  <th className="num">Subtotal</th>
                                </tr>
                              </thead>
                              <tbody>
                                {v.itens.map((i) => (
                                  <tr key={i.produtoId}>
                                    <td>{i.produtoNome}</td>
                                    <td className="num">{i.quantidade}</td>
                                    <td className="num">{formatarMoeda(i.precoUnitario)}</td>
                                    <td className="num">{formatarMoeda(i.subtotal)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
      </section>
    </>
  );
}

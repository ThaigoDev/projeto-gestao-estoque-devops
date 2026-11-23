import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { GraficoColunas } from '../components/Graficos';
import { CabecalhoPagina, CartaoIndicador, Carregando, MensagemErro, SeloEstoque, Vazio } from '../components/Ui';
import { IconeAlerta, IconeCaixa, IconeCalendario, IconeDinheiro } from '../components/Icones';
import {
  NOMES_PAGAMENTO,
  diasAtras,
  formatarDataHora,
  formatarDiaCurto,
  formatarMoeda,
  formatarNumero,
} from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

async function carregarDashboard() {
  const [resumo, semana, estoqueBaixo, vendas] = await Promise.all([
    api.dashboard(),
    api.relatorios.faturamento(diasAtras(6)),
    api.produtos.listar({ estoque: 'baixo' }),
    api.vendas.listar(),
  ]);
  return { resumo, semana, estoqueBaixo, ultimasVendas: vendas.slice(0, 5) };
}

export function DashboardPage() {
  // Atualiza sozinho a cada 30s para refletir vendas e o trabalho do Worker.
  const { dados, erro, carregando, recarregar } = useCarregar(carregarDashboard, [], 30000);

  return (
    <>
      <CabecalhoPagina
        titulo="Dashboard"
        descricao="Visão geral do estoque e das vendas"
        acoes={
          <>
            <button type="button" className="btn btn-secundario" onClick={recarregar}>
              Atualizar
            </button>
            <Link to="/vendas/nova" className="btn">
              Nova venda
            </Link>
          </>
        }
      />

      {erro && <MensagemErro texto={erro} aoTentarDeNovo={recarregar} />}
      {!dados && carregando && <Carregando />}

      {dados && (
        <>
          <section className="grade-indicadores">
            <CartaoIndicador
              rotulo="Faturamento hoje"
              valor={formatarMoeda(dados.resumo.faturamentoHoje)}
              detalhe={`${formatarNumero(dados.resumo.vendasHoje)} venda(s)`}
              destaque="verde"
              icone={<IconeDinheiro />}
            />
            <CartaoIndicador
              rotulo="Faturamento no mês"
              valor={formatarMoeda(dados.resumo.faturamentoMes)}
              detalhe={`${formatarNumero(dados.resumo.vendasMes)} venda(s)`}
              destaque="azul"
              icone={<IconeCalendario />}
            />
            <CartaoIndicador
              rotulo="Produtos cadastrados"
              valor={formatarNumero(dados.resumo.totalProdutos)}
              detalhe={`${formatarNumero(dados.resumo.produtosEstoqueBaixo)} com estoque baixo`}
              destaque={dados.resumo.produtosEstoqueBaixo > 0 ? 'amarelo' : 'padrao'}
              icone={<IconeCaixa />}
            />
            <CartaoIndicador
              rotulo="Sem estoque"
              valor={formatarNumero(dados.resumo.produtosSemEstoque)}
              detalhe={
                <Link to="/reposicoes">{formatarNumero(dados.resumo.reposicoesPendentes)} reposição(ões) pendente(s)</Link>
              }
              destaque={dados.resumo.produtosSemEstoque > 0 ? 'vermelho' : 'padrao'}
              icone={<IconeAlerta />}
            />
          </section>

          <section className="painel">
            <div className="painel-cabecalho">
              <h2>Faturamento dos últimos 7 dias</h2>
              <strong>{formatarMoeda(dados.semana.faturamentoTotal)}</strong>
            </div>
            <GraficoColunas
              formatar={formatarMoeda}
              pontos={dados.semana.porDia.map((d) => ({
                rotulo: formatarDiaCurto(d.data),
                valor: d.faturamento,
                dica: `${formatarDiaCurto(d.data)}: ${formatarMoeda(d.faturamento)} (${d.quantidadeVendas} venda(s))`,
              }))}
            />
          </section>

          <div className="grade-2">
            <section className="painel">
              <div className="painel-cabecalho">
                <h2>Estoque baixo ou zerado</h2>
                <Link to="/produtos">Ver produtos</Link>
              </div>
              {dados.estoqueBaixo.length === 0 ? (
                <Vazio texto="Nenhum produto com estoque baixo." />
              ) : (
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th className="num">Estoque</th>
                      <th className="num">Mínimo</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {dados.estoqueBaixo.slice(0, 8).map((p) => (
                      <tr key={p.id}>
                        <td>{p.nome}</td>
                        <td className="num">{p.quantidadeEstoque}</td>
                        <td className="num">{p.estoqueMinimo}</td>
                        <td>
                          <SeloEstoque produto={p} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>

            <section className="painel">
              <div className="painel-cabecalho">
                <h2>Últimas vendas</h2>
                <Link to="/vendas">Ver histórico</Link>
              </div>
              {dados.ultimasVendas.length === 0 ? (
                <Vazio texto="Nenhuma venda registrada ainda." />
              ) : (
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Pagamento</th>
                      <th className="num">Itens</th>
                      <th className="num">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.ultimasVendas.map((v) => (
                      <tr key={v.id}>
                        <td>{formatarDataHora(v.dataVenda)}</td>
                        <td>{NOMES_PAGAMENTO[v.formaPagamento]}</td>
                        <td className="num">{v.quantidadeItens}</td>
                        <td className="num">{formatarMoeda(v.valorTotal)}</td>
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

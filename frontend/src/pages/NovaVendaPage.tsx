import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, mensagemDeErro } from '../api/client';
import type { FormaPagamento, Produto, Venda } from '../api/types';
import { useAvisos } from '../components/Avisos';
import { CabecalhoPagina, Carregando, MensagemErro, Vazio } from '../components/Ui';
import { NOMES_PAGAMENTO, formatarDataHora, formatarMoeda } from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
}

export function NovaVendaPage() {
  const avisar = useAvisos();
  const produtos = useCarregar(() => api.produtos.listar());

  const [carrinho, setCarrinho] = useState<ItemCarrinho[]>([]);
  const [produtoId, setProdutoId] = useState('');
  const [quantidade, setQuantidade] = useState('1');
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento>('PIX');
  const [cliente, setCliente] = useState('');
  const [erro, setErro] = useState<string>();
  const [enviando, setEnviando] = useState(false);
  const [ultimaVenda, setUltimaVenda] = useState<Venda>();

  const noCarrinho = (id: string) => carrinho.find((i) => i.produto.id === id)?.quantidade ?? 0;
  const disponivel = (p: Produto) => p.quantidadeEstoque - noCarrinho(p.id);
  const total = carrinho.reduce((soma, i) => soma + i.produto.precoVenda * i.quantidade, 0);

  function adicionar(e: FormEvent) {
    e.preventDefault();
    setErro(undefined);
    const produto = produtos.dados?.find((p) => p.id === produtoId);
    const qtd = Number(quantidade);
    if (!produto) return setErro('Escolha um produto.');
    if (!Number.isInteger(qtd) || qtd < 1) return setErro('A quantidade deve ser um número inteiro maior que zero.');
    if (qtd > disponivel(produto)) {
      return setErro(`Estoque insuficiente para "${produto.nome}": disponível ${disponivel(produto)}.`);
    }
    setCarrinho((c) =>
      c.some((i) => i.produto.id === produto.id)
        ? c.map((i) => (i.produto.id === produto.id ? { ...i, quantidade: i.quantidade + qtd } : i))
        : [...c, { produto, quantidade: qtd }],
    );
    setProdutoId('');
    setQuantidade('1');
  }

  function alterarQuantidade(id: string, qtd: number) {
    setCarrinho((c) =>
      c.map((i) => (i.produto.id === id ? { ...i, quantidade: Math.max(1, Math.min(qtd, i.produto.quantidadeEstoque)) } : i)),
    );
  }

  const remover = (id: string) => setCarrinho((c) => c.filter((i) => i.produto.id !== id));

  async function finalizar() {
    setErro(undefined);
    if (carrinho.length === 0) return setErro('Adicione pelo menos um produto.');
    setEnviando(true);
    try {
      const venda = await api.vendas.registrar({
        itens: carrinho.map((i) => ({ produtoId: i.produto.id, quantidade: i.quantidade })),
        formaPagamento,
        cliente: cliente.trim() || undefined,
      });
      avisar(`Venda registrada: ${formatarMoeda(venda.valorTotal)}.`);
      setUltimaVenda(venda);
      setCarrinho([]);
      setCliente('');
      produtos.recarregar();
    } catch (err) {
      // 422 = estoque insuficiente (a API recusa a venda inteira); recarrega para mostrar o estoque real.
      setErro(mensagemDeErro(err));
      produtos.recarregar();
    } finally {
      setEnviando(false);
    }
  }

  const selecionado = produtos.dados?.find((p) => p.id === produtoId);

  return (
    <>
      <CabecalhoPagina titulo="Nova venda" descricao="Monte o carrinho e registre a venda; o estoque é baixado automaticamente" />

      {produtos.erro && <MensagemErro texto={produtos.erro} aoTentarDeNovo={produtos.recarregar} />}
      {!produtos.dados && produtos.carregando && <Carregando />}

      {ultimaVenda && (
        <div className="alerta alerta-sucesso">
          Venda de <strong>{formatarMoeda(ultimaVenda.valorTotal)}</strong> registrada em{' '}
          {formatarDataHora(ultimaVenda.dataVenda)} ({ultimaVenda.quantidadeItens} item(ns),{' '}
          {NOMES_PAGAMENTO[ultimaVenda.formaPagamento]}). <Link to="/vendas">Ver histórico</Link>
        </div>
      )}

      {produtos.dados && (
        <div className="grade-lateral">
          <section className="painel">
            <h2>Adicionar produto</h2>
            <form className="formulario" onSubmit={adicionar}>
              <label className="campo">
                Produto
                <select value={produtoId} onChange={(e) => setProdutoId(e.target.value)}>
                  <option value="">Selecione...</option>
                  {produtos.dados.map((p) => (
                    <option key={p.id} value={p.id} disabled={disponivel(p) <= 0}>
                      {p.nome} — {formatarMoeda(p.precoVenda)} ({disponivel(p) <= 0 ? 'sem estoque' : `${disponivel(p)} disp.`})
                    </option>
                  ))}
                </select>
              </label>
              <div className="linha-campos">
                <label className="campo campo-pequeno">
                  Quantidade
                  <input
                    type="number"
                    min={1}
                    max={selecionado ? disponivel(selecionado) : undefined}
                    step={1}
                    value={quantidade}
                    onChange={(e) => setQuantidade(e.target.value)}
                  />
                </label>
                <div className="campo campo-acao">
                  <button type="submit" className="btn btn-secundario">
                    Adicionar ao carrinho
                  </button>
                </div>
              </div>
              {selecionado && (
                <p className="dica">
                  Preço: {formatarMoeda(selecionado.precoVenda)} · disponível: {disponivel(selecionado)}
                </p>
              )}
            </form>
          </section>

          <section className="painel">
            <h2>Carrinho</h2>
            {carrinho.length === 0 ? (
              <Vazio texto="Nenhum produto no carrinho." />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th className="num">Preço</th>
                      <th className="num">Qtd.</th>
                      <th className="num">Subtotal</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {carrinho.map((i) => (
                      <tr key={i.produto.id}>
                        <td>{i.produto.nome}</td>
                        <td className="num">{formatarMoeda(i.produto.precoVenda)}</td>
                        <td className="num">
                          <input
                            className="input-qtd"
                            type="number"
                            min={1}
                            max={i.produto.quantidadeEstoque}
                            value={i.quantidade}
                            onChange={(e) => alterarQuantidade(i.produto.id, Number(e.target.value))}
                            aria-label={`Quantidade de ${i.produto.nome}`}
                          />
                        </td>
                        <td className="num">{formatarMoeda(i.produto.precoVenda * i.quantidade)}</td>
                        <td className="acoes-tabela">
                          <button type="button" className="btn btn-perigo btn-pequeno" onClick={() => remover(i.produto.id)}>
                            Remover
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="formulario">
              <div className="linha-campos">
                <label className="campo">
                  Forma de pagamento
                  <select value={formaPagamento} onChange={(e) => setFormaPagamento(e.target.value as FormaPagamento)}>
                    {Object.entries(NOMES_PAGAMENTO).map(([valor, nome]) => (
                      <option key={valor} value={valor}>
                        {nome}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="campo">
                  Cliente (opcional)
                  <input value={cliente} onChange={(e) => setCliente(e.target.value)} maxLength={120} />
                </label>
              </div>

              <div className="total-venda">
                <span>Total</span>
                <strong>{formatarMoeda(total)}</strong>
              </div>
              <p className="dica">O valor final é calculado pela API com o preço atual de cada produto.</p>

              {erro && <div className="alerta alerta-erro">{erro}</div>}

              <div className="rodape-formulario">
                <button
                  type="button"
                  className="btn btn-secundario"
                  onClick={() => setCarrinho([])}
                  disabled={carrinho.length === 0 || enviando}
                >
                  Limpar
                </button>
                <button type="button" className="btn" onClick={finalizar} disabled={carrinho.length === 0 || enviando}>
                  {enviando ? 'Registrando...' : 'Finalizar venda'}
                </button>
              </div>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

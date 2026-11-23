import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, mensagemDeErro } from '../api/client';
import { useAvisos } from '../components/Avisos';
import { CabecalhoPagina, Carregando, MensagemErro, Vazio } from '../components/Ui';
import { formatarDataHora, formatarMoeda } from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

const CAMPOS_VAZIOS = { quantidade: '', custoUnitario: '', fornecedor: '', observacao: '' };

export function EntradasPage() {
  const avisar = useAvisos();
  const [params] = useSearchParams();

  const produtos = useCarregar(() => api.produtos.listar());
  const entradas = useCarregar(() => api.entradas.listar());

  const [produtoId, setProdutoId] = useState(params.get('produto') ?? '');
  const [campos, setCampos] = useState(CAMPOS_VAZIOS);
  const [erro, setErro] = useState<string>();
  const [salvando, setSalvando] = useState(false);

  const produtoSelecionado = produtos.dados?.find((p) => p.id === produtoId);

  const alterar = (campo: keyof typeof campos) => (e: { target: { value: string } }) =>
    setCampos((c) => ({ ...c, [campo]: e.target.value }));

  async function registrar(e: FormEvent) {
    e.preventDefault();
    setErro(undefined);

    const quantidade = Number(campos.quantidade);
    if (!produtoId) return setErro('Escolha o produto que chegou.');
    if (!Number.isInteger(quantidade) || quantidade < 1) return setErro('A quantidade deve ser um número inteiro maior que zero.');

    setSalvando(true);
    try {
      const entrada = await api.entradas.registrar({
        produtoId,
        quantidade,
        custoUnitario: campos.custoUnitario ? Number(campos.custoUnitario) : undefined,
        fornecedor: campos.fornecedor.trim() || undefined,
        observacao: campos.observacao.trim() || undefined,
      });
      avisar(`Entrada registrada: +${entrada.quantidade} ${entrada.produtoNome} (estoque agora: ${entrada.estoqueResultante}).`);
      setCampos(CAMPOS_VAZIOS);
      produtos.recarregar();
      entradas.recarregar();
    } catch (err) {
      setErro(mensagemDeErro(err));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <>
      <CabecalhoPagina titulo="Entrada de mercadorias" descricao="Registre a chegada de produtos para repor o estoque" />

      <div className="grade-lateral">
        <section className="painel">
          <h2>Nova entrada</h2>
          {produtos.erro && <MensagemErro texto={produtos.erro} aoTentarDeNovo={produtos.recarregar} />}
          <form className="formulario" onSubmit={registrar}>
            <label className="campo">
              Produto *
              <select value={produtoId} onChange={(e) => setProdutoId(e.target.value)} required>
                <option value="">Selecione...</option>
                {produtos.dados?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nome} {p.codigo ? `(${p.codigo})` : ''} — estoque: {p.quantidadeEstoque}
                  </option>
                ))}
              </select>
            </label>

            {produtoSelecionado && (
              <p className="dica">
                Estoque atual: <strong>{produtoSelecionado.quantidadeEstoque}</strong> · mínimo:{' '}
                {produtoSelecionado.estoqueMinimo}
                {campos.quantidade && Number(campos.quantidade) > 0 && (
                  <>
                    {' '}
                    · depois da entrada: <strong>{produtoSelecionado.quantidadeEstoque + Number(campos.quantidade)}</strong>
                  </>
                )}
              </p>
            )}

            <div className="linha-campos">
              <label className="campo">
                Quantidade *
                <input type="number" min={1} step={1} value={campos.quantidade} onChange={alterar('quantidade')} required />
              </label>
              <label className="campo">
                Custo unitário (R$)
                <input type="number" min={0} step={0.01} value={campos.custoUnitario} onChange={alterar('custoUnitario')} />
              </label>
            </div>
            <label className="campo">
              Fornecedor
              <input value={campos.fornecedor} onChange={alterar('fornecedor')} maxLength={120} />
            </label>
            <label className="campo">
              Observação
              <textarea value={campos.observacao} onChange={alterar('observacao')} maxLength={300} rows={2} />
            </label>

            {erro && <div className="alerta alerta-erro">{erro}</div>}

            <div className="rodape-formulario">
              <button type="submit" className="btn" disabled={salvando}>
                {salvando ? 'Registrando...' : 'Registrar entrada'}
              </button>
            </div>
          </form>
        </section>

        <section className="painel">
          <h2>Histórico de entradas</h2>
          {entradas.erro && <MensagemErro texto={entradas.erro} aoTentarDeNovo={entradas.recarregar} />}
          {!entradas.dados && entradas.carregando && <Carregando />}
          {entradas.dados &&
            (entradas.dados.length === 0 ? (
              <Vazio texto="Nenhuma entrada registrada ainda." />
            ) : (
              <div className="tabela-rolagem">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Produto</th>
                      <th className="num">Qtd.</th>
                      <th className="num">Custo un.</th>
                      <th>Fornecedor</th>
                      <th className="num">Estoque após</th>
                    </tr>
                  </thead>
                  <tbody>
                    {entradas.dados.map((en) => (
                      <tr key={en.id}>
                        <td>{formatarDataHora(en.dataEntrada)}</td>
                        <td>
                          {en.produtoNome}
                          {en.observacao && <div className="texto-suave pequeno">{en.observacao}</div>}
                        </td>
                        <td className="num texto-verde">+{en.quantidade}</td>
                        <td className="num">{en.custoUnitario != null ? formatarMoeda(en.custoUnitario) : '-'}</td>
                        <td>{en.fornecedor ?? '-'}</td>
                        <td className="num">{en.estoqueResultante}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
        </section>
      </div>
    </>
  );
}

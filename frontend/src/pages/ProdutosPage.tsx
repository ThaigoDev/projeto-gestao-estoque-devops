import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, mensagemDeErro } from '../api/client';
import type { Produto } from '../api/types';
import { useAvisos } from '../components/Avisos';
import { Modal } from '../components/Modal';
import { CabecalhoPagina, Carregando, MensagemErro, SeloEstoque, Vazio } from '../components/Ui';
import { formatarMoeda } from '../utils/format';
import { useCarregar } from '../utils/useCarregar';
import { ProdutoForm } from './ProdutoForm';

type FiltroEstoque = 'todos' | 'baixo' | 'zerado';

/** Remove acentos e caixa para a busca ("acucar" encontra "Açúcar"). */
const normalizar = (s: string) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

export function ProdutosPage() {
  const avisar = useAvisos();
  const { dados: produtos, erro, carregando, recarregar } = useCarregar(() => api.produtos.listar());

  const [busca, setBusca] = useState('');
  const [categoria, setCategoria] = useState('');
  const [filtroEstoque, setFiltroEstoque] = useState<FiltroEstoque>('todos');
  // undefined = modal fechado; null = novo produto; Produto = edicao
  const [emEdicao, setEmEdicao] = useState<Produto | null>();

  const categorias = useMemo(
    () => [...new Set((produtos ?? []).map((p) => p.categoria).filter((c): c is string => Boolean(c)))].sort(),
    [produtos],
  );

  const filtrados = useMemo(() => {
    const termo = normalizar(busca.trim());
    return (produtos ?? []).filter((p) => {
      if (termo && !normalizar(`${p.nome} ${p.codigo ?? ''}`).includes(termo)) return false;
      if (categoria && p.categoria !== categoria) return false;
      if (filtroEstoque === 'zerado' && !p.semEstoque) return false;
      if (filtroEstoque === 'baixo' && !p.estoqueBaixo) return false;
      return true;
    });
  }, [produtos, busca, categoria, filtroEstoque]);

  async function desativar(produto: Produto) {
    const ok = window.confirm(
      `Remover "${produto.nome}" da lista de produtos?\n\nEle continua aparecendo no histórico de vendas.`,
    );
    if (!ok) return;
    try {
      await api.produtos.desativar(produto.id);
      avisar(`Produto "${produto.nome}" removido.`);
      recarregar();
    } catch (e) {
      avisar(mensagemDeErro(e), 'erro');
    }
  }

  function aoSalvar(produto: Produto) {
    avisar(emEdicao ? `Produto "${produto.nome}" atualizado.` : `Produto "${produto.nome}" cadastrado.`);
    setEmEdicao(undefined);
    recarregar();
  }

  return (
    <>
      <CabecalhoPagina
        titulo="Produtos"
        descricao="Cadastro e consulta dos produtos do comércio"
        acoes={
          <button type="button" className="btn" onClick={() => setEmEdicao(null)}>
            + Novo produto
          </button>
        }
      />

      <section className="painel">
        <div className="filtros">
          <input
            type="search"
            placeholder="Buscar por nome ou código..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            aria-label="Buscar produto"
          />
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)} aria-label="Categoria">
            <option value="">Todas as categorias</option>
            {categorias.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <div className="segmentado" role="group" aria-label="Situação do estoque">
            {(
              [
                ['todos', 'Todos'],
                ['baixo', 'Estoque baixo'],
                ['zerado', 'Sem estoque'],
              ] as const
            ).map(([valor, rotulo]) => (
              <button
                key={valor}
                type="button"
                className={filtroEstoque === valor ? 'ativo' : ''}
                onClick={() => setFiltroEstoque(valor)}
              >
                {rotulo}
              </button>
            ))}
          </div>
        </div>

        {erro && <MensagemErro texto={erro} aoTentarDeNovo={recarregar} />}
        {!produtos && carregando && <Carregando />}

        {produtos &&
          (filtrados.length === 0 ? (
            <Vazio texto={produtos.length === 0 ? 'Nenhum produto cadastrado ainda.' : 'Nenhum produto encontrado.'} />
          ) : (
            <div className="tabela-rolagem">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Nome</th>
                    <th>Categoria</th>
                    <th className="num">Preço</th>
                    <th className="num">Estoque</th>
                    <th className="num">Mínimo</th>
                    <th>Situação</th>
                    <th className="acoes-tabela">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((p) => (
                    <tr key={p.id}>
                      <td className="texto-suave">{p.codigo ?? '-'}</td>
                      <td>
                        <strong>{p.nome}</strong>
                        {p.descricao && <div className="texto-suave pequeno">{p.descricao}</div>}
                      </td>
                      <td>{p.categoria ?? '-'}</td>
                      <td className="num">{formatarMoeda(p.precoVenda)}</td>
                      <td className="num">
                        <strong>{p.quantidadeEstoque}</strong>
                      </td>
                      <td className="num">{p.estoqueMinimo}</td>
                      <td>
                        <SeloEstoque produto={p} />
                      </td>
                      <td className="acoes-tabela">
                        <Link to={`/entradas?produto=${p.id}`} className="btn btn-secundario btn-pequeno">
                          Entrada
                        </Link>
                        <button type="button" className="btn btn-secundario btn-pequeno" onClick={() => setEmEdicao(p)}>
                          Editar
                        </button>
                        <button type="button" className="btn btn-perigo btn-pequeno" onClick={() => desativar(p)}>
                          Remover
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        {produtos && (
          <p className="texto-suave pequeno rodape-tabela">
            {filtrados.length} de {produtos.length} produto(s)
          </p>
        )}
      </section>

      <Modal
        titulo={emEdicao ? `Editar ${emEdicao.nome}` : 'Novo produto'}
        aberto={emEdicao !== undefined}
        aoFechar={() => setEmEdicao(undefined)}
      >
        {emEdicao !== undefined && (
          <ProdutoForm
            produto={emEdicao ?? undefined}
            categorias={categorias}
            aoSalvar={aoSalvar}
            aoCancelar={() => setEmEdicao(undefined)}
          />
        )}
      </Modal>
    </>
  );
}

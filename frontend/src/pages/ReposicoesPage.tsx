import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { CabecalhoPagina, Carregando, MensagemErro, Vazio } from '../components/Ui';
import { formatarDataHora } from '../utils/format';
import { useCarregar } from '../utils/useCarregar';

type Aba = 'PENDENTE' | 'RESOLVIDA' | 'TODAS';

const ABAS: [Aba, string][] = [
  ['PENDENTE', 'Pendentes'],
  ['RESOLVIDA', 'Resolvidas'],
  ['TODAS', 'Todas'],
];

export function ReposicoesPage() {
  const [aba, setAba] = useState<Aba>('PENDENTE');
  // As reposicoes sao geradas pelo Worker em segundo plano, entao a tela se atualiza sozinha.
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.reposicoes(aba), [aba], 15000);

  return (
    <>
      <CabecalhoPagina
        titulo="Reposição de estoque"
        descricao="Produtos sem estoque detectados automaticamente pelo Worker (atualiza a cada 15 s)"
        acoes={
          <button type="button" className="btn btn-secundario" onClick={recarregar}>
            Atualizar
          </button>
        }
      />

      <section className="painel">
        <div className="filtros">
          <div className="segmentado" role="tablist">
            {ABAS.map(([valor, rotulo]) => (
              <button
                key={valor}
                type="button"
                role="tab"
                aria-selected={aba === valor}
                className={aba === valor ? 'ativo' : ''}
                onClick={() => setAba(valor)}
              >
                {rotulo}
              </button>
            ))}
          </div>
        </div>

        {erro && <MensagemErro texto={erro} aoTentarDeNovo={recarregar} />}
        {!dados && carregando && <Carregando />}

        {dados &&
          (dados.length === 0 ? (
            <Vazio
              texto={aba === 'PENDENTE' ? 'Nenhuma reposição pendente. Estoque em dia!' : 'Nenhuma reposição encontrada.'}
            />
          ) : (
            <div className="tabela-rolagem">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Produto</th>
                    <th className="num">Estoque no registro</th>
                    <th>Situação</th>
                    <th>Detectado em</th>
                    <th>Resolvido em</th>
                    <th className="acoes-tabela" />
                  </tr>
                </thead>
                <tbody>
                  {dados.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <strong>{r.produtoNome}</strong>
                      </td>
                      <td className="num">{r.quantidadeAtual}</td>
                      <td>
                        {r.status === 'PENDENTE' ? (
                          <span className="selo selo-vermelho">Pendente</span>
                        ) : (
                          <span className="selo selo-verde">Resolvida</span>
                        )}
                      </td>
                      <td>{formatarDataHora(r.dataRegistro)}</td>
                      <td>{r.dataResolucao ? formatarDataHora(r.dataResolucao) : '-'}</td>
                      <td className="acoes-tabela">
                        {r.status === 'PENDENTE' && (
                          <Link to={`/entradas?produto=${r.produtoId}`} className="btn btn-pequeno">
                            Registrar entrada
                          </Link>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
      </section>
    </>
  );
}

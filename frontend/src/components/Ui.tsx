import type { ReactNode } from 'react';
import type { Produto } from '../api/types';
import { IconeAtualizar, IconeDesconectado, IconeVazio } from './Icones';

export function CabecalhoPagina({ titulo, descricao, acoes }: { titulo: string; descricao?: string; acoes?: ReactNode }) {
  return (
    <header className="cabecalho-pagina">
      <div>
        <h1>{titulo}</h1>
        {descricao && <p className="texto-suave">{descricao}</p>}
      </div>
      {acoes && <div className="acoes">{acoes}</div>}
    </header>
  );
}

type Destaque = 'padrao' | 'verde' | 'amarelo' | 'vermelho' | 'azul';

export function CartaoIndicador({
  rotulo,
  valor,
  detalhe,
  destaque = 'padrao',
  icone,
}: {
  rotulo: string;
  valor: ReactNode;
  detalhe?: ReactNode;
  destaque?: Destaque;
  icone?: ReactNode;
}) {
  return (
    <div className={`cartao-indicador destaque-${destaque}`}>
      <div className="cartao-topo">
        <span className="rotulo">{rotulo}</span>
        {icone && <span className="cartao-icone">{icone}</span>}
      </div>
      <strong className="valor">{valor}</strong>
      {detalhe && <span className="detalhe">{detalhe}</span>}
    </div>
  );
}

export function Carregando({ texto = 'Carregando...' }: { texto?: string }) {
  return (
    <div className="estado">
      <span className="spinner" aria-hidden="true" /> {texto}
    </div>
  );
}

export function MensagemErro({ texto, aoTentarDeNovo }: { texto: string; aoTentarDeNovo?: () => void }) {
  return (
    <div className="alerta alerta-erro" role="alert">
      <span className="alerta-icone">
        <IconeDesconectado tamanho={20} />
      </span>
      <span className="alerta-texto">{texto}</span>
      {aoTentarDeNovo && (
        <button type="button" className="btn btn-secundario btn-pequeno" onClick={aoTentarDeNovo}>
          <IconeAtualizar tamanho={14} /> Tentar de novo
        </button>
      )}
    </div>
  );
}

export function Vazio({ texto }: { texto: string }) {
  return (
    <div className="estado estado-vazio">
      <span className="estado-icone">
        <IconeVazio tamanho={26} />
      </span>
      {texto}
    </div>
  );
}

/** Selo de situacao do estoque de um produto. */
export function SeloEstoque({ produto }: { produto: Pick<Produto, 'quantidadeEstoque' | 'semEstoque' | 'estoqueBaixo'> }) {
  if (produto.semEstoque) return <span className="selo selo-vermelho">Sem estoque</span>;
  if (produto.estoqueBaixo) return <span className="selo selo-amarelo">Estoque baixo</span>;
  return <span className="selo selo-verde">OK</span>;
}

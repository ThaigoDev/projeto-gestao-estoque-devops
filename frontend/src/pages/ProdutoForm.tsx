import { useState, type FormEvent } from 'react';
import { api, mensagemDeErro } from '../api/client';
import type { Produto, ProdutoRequest } from '../api/types';

interface Props {
  /** Se vier, o formulario edita esse produto; senao, cadastra um novo. */
  produto?: Produto;
  categorias: string[];
  aoSalvar: (produto: Produto) => void;
  aoCancelar: () => void;
}

const texto = (v: string) => (v.trim() === '' ? undefined : v.trim());
const numero = (v: string) => (v.trim() === '' ? undefined : Number(v.replace(',', '.')));

export function ProdutoForm({ produto, categorias, aoSalvar, aoCancelar }: Props) {
  const editando = Boolean(produto);
  const [campos, setCampos] = useState({
    codigo: produto?.codigo ?? '',
    nome: produto?.nome ?? '',
    categoria: produto?.categoria ?? '',
    descricao: produto?.descricao ?? '',
    precoVenda: produto?.precoVenda?.toString() ?? '',
    precoCusto: produto?.precoCusto?.toString() ?? '',
    estoqueMinimo: produto?.estoqueMinimo?.toString() ?? '5',
    estoqueInicial: '0',
  });
  const [erro, setErro] = useState<string>();
  const [salvando, setSalvando] = useState(false);

  const alterar = (campo: keyof typeof campos) => (e: { target: { value: string } }) =>
    setCampos((c) => ({ ...c, [campo]: e.target.value }));

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(undefined);

    const precoVenda = numero(campos.precoVenda);
    if (!campos.nome.trim()) return setErro('Informe o nome do produto.');
    if (precoVenda === undefined || Number.isNaN(precoVenda) || precoVenda <= 0) {
      return setErro('Informe um preço de venda maior que zero.');
    }

    const dados: ProdutoRequest = {
      codigo: texto(campos.codigo),
      nome: campos.nome.trim(),
      categoria: texto(campos.categoria),
      descricao: texto(campos.descricao),
      precoVenda,
      precoCusto: numero(campos.precoCusto),
      estoqueMinimo: numero(campos.estoqueMinimo),
      estoqueInicial: editando ? undefined : numero(campos.estoqueInicial),
    };

    setSalvando(true);
    try {
      const salvo = produto ? await api.produtos.atualizar(produto.id, dados) : await api.produtos.criar(dados);
      aoSalvar(salvo);
    } catch (err) {
      setErro(mensagemDeErro(err));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <div className="linha-campos">
        <label className="campo">
          Nome *
          <input value={campos.nome} onChange={alterar('nome')} maxLength={120} required autoFocus />
        </label>
        <label className="campo campo-pequeno">
          Código
          <input value={campos.codigo} onChange={alterar('codigo')} maxLength={50} placeholder="Ex.: ARZ-5KG" />
        </label>
      </div>

      <div className="linha-campos">
        <label className="campo">
          Categoria
          <input value={campos.categoria} onChange={alterar('categoria')} maxLength={60} list="lista-categorias" />
          <datalist id="lista-categorias">
            {categorias.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </label>
        <label className="campo campo-pequeno">
          Estoque mínimo
          <input type="number" min={0} step={1} value={campos.estoqueMinimo} onChange={alterar('estoqueMinimo')} />
        </label>
      </div>

      <div className="linha-campos">
        <label className="campo">
          Preço de venda (R$) *
          <input type="number" min={0.01} step={0.01} value={campos.precoVenda} onChange={alterar('precoVenda')} required />
        </label>
        <label className="campo">
          Preço de custo (R$)
          <input type="number" min={0} step={0.01} value={campos.precoCusto} onChange={alterar('precoCusto')} />
        </label>
        {!editando && (
          <label className="campo campo-pequeno">
            Estoque inicial
            <input type="number" min={0} step={1} value={campos.estoqueInicial} onChange={alterar('estoqueInicial')} />
          </label>
        )}
      </div>

      <label className="campo">
        Descrição
        <textarea value={campos.descricao} onChange={alterar('descricao')} maxLength={500} rows={2} />
      </label>

      {editando && (
        <p className="dica">
          O estoque não é alterado por aqui. Use <strong>Entrada de mercadorias</strong> para repor; as vendas dão baixa
          automaticamente.
        </p>
      )}

      {erro && <div className="alerta alerta-erro">{erro}</div>}

      <div className="rodape-formulario">
        <button type="button" className="btn btn-secundario" onClick={aoCancelar}>
          Cancelar
        </button>
        <button type="submit" className="btn" disabled={salvando}>
          {salvando ? 'Salvando...' : editando ? 'Salvar alterações' : 'Cadastrar produto'}
        </button>
      </div>
    </form>
  );
}

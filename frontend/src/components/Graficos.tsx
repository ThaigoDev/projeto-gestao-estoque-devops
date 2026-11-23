export interface Ponto {
  rotulo: string;
  valor: number;
  /** Texto do tooltip; se nao vier, usa o valor. */
  dica?: string;
}

/** Grafico de colunas simples (sem biblioteca), ex.: faturamento por dia. */
export function GraficoColunas({ pontos, formatar = String }: { pontos: Ponto[]; formatar?: (v: number) => string }) {
  const maximo = Math.max(...pontos.map((p) => p.valor), 0);
  // Com muitos dias, mostra so alguns rotulos no eixo para nao embolar.
  const passoRotulo = Math.max(1, Math.ceil(pontos.length / 10));

  return (
    <div className="grafico-colunas" role="img" aria-label="Gráfico de colunas">
      <div className="colunas">
        {pontos.map((p, i) => (
          <div key={p.rotulo + i} className="coluna" title={p.dica ?? `${p.rotulo}: ${formatar(p.valor)}`}>
            <div className="barra" style={{ height: maximo > 0 ? `${(p.valor / maximo) * 100}%` : '0%' }} />
          </div>
        ))}
      </div>
      <div className="eixo">
        {pontos.map((p, i) => (
          <span key={p.rotulo + i}>{i % passoRotulo === 0 ? p.rotulo : ''}</span>
        ))}
      </div>
    </div>
  );
}

/** Barras horizontais com rotulo e valor, ex.: faturamento por forma de pagamento. */
export function GraficoBarras({ pontos, formatar = String }: { pontos: Ponto[]; formatar?: (v: number) => string }) {
  const maximo = Math.max(...pontos.map((p) => p.valor), 0);
  return (
    <ul className="grafico-barras">
      {pontos.map((p) => (
        <li key={p.rotulo}>
          <div className="linha-rotulo">
            <span>{p.rotulo}</span>
            <strong>{formatar(p.valor)}</strong>
          </div>
          <div className="trilho">
            <div className="barra" style={{ width: maximo > 0 ? `${(p.valor / maximo) * 100}%` : '0%' }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

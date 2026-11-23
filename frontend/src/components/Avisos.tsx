import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Tipo = 'sucesso' | 'erro' | 'info';

interface Aviso {
  id: number;
  tipo: Tipo;
  texto: string;
}

type Avisar = (texto: string, tipo?: Tipo) => void;

const AvisosContext = createContext<Avisar>(() => {});

/** Notificacoes rapidas no canto da tela (ex.: "Venda registrada"). */
export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);

  const remover = useCallback((id: number) => setAvisos((lista) => lista.filter((a) => a.id !== id)), []);

  const avisar = useCallback<Avisar>(
    (texto, tipo = 'sucesso') => {
      const id = Date.now() + Math.random();
      setAvisos((lista) => [...lista, { id, tipo, texto }]);
      setTimeout(() => remover(id), tipo === 'erro' ? 7000 : 4000);
    },
    [remover],
  );

  return (
    <AvisosContext.Provider value={avisar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`} onClick={() => remover(a.id)}>
            {a.texto}
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  );
}

export const useAvisos = () => useContext(AvisosContext);

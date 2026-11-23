import { useEffect, type ReactNode } from 'react';

interface Props {
  titulo: string;
  aberto: boolean;
  aoFechar: () => void;
  children: ReactNode;
}

export function Modal({ titulo, aberto, aoFechar, children }: Props) {
  useEffect(() => {
    if (!aberto) return;
    const aoTeclar = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar();
    window.addEventListener('keydown', aoTeclar);
    return () => window.removeEventListener('keydown', aoTeclar);
  }, [aberto, aoFechar]);

  if (!aberto) return null;

  return (
    <div className="modal-fundo" onMouseDown={aoFechar}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={titulo} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-cabecalho">
          <h2>{titulo}</h2>
          <button type="button" className="btn-icone" onClick={aoFechar} aria-label="Fechar">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

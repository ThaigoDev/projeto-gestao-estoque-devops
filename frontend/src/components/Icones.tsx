import type { ReactNode } from 'react';

/** Icones em SVG (traco), no estilo "outline", sem dependencia externa. */
function Icone({ children, tamanho = 18 }: { children: ReactNode; tamanho?: number }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

type P = { tamanho?: number };

export const IconePainel = (p: P) => (
  <Icone {...p}>
    <rect x="3" y="3" width="7" height="9" rx="1.5" />
    <rect x="14" y="3" width="7" height="5" rx="1.5" />
    <rect x="14" y="12" width="7" height="9" rx="1.5" />
    <rect x="3" y="16" width="7" height="5" rx="1.5" />
  </Icone>
);

export const IconeCarrinho = (p: P) => (
  <Icone {...p}>
    <circle cx="9" cy="20" r="1.4" />
    <circle cx="18" cy="20" r="1.4" />
    <path d="M2.5 3h2.6l2.4 11.2a1.8 1.8 0 0 0 1.8 1.4h8.4a1.8 1.8 0 0 0 1.8-1.4L21 7H6" />
  </Icone>
);

export const IconeRecibo = (p: P) => (
  <Icone {...p}>
    <path d="M5 3h14v18l-2.5-1.6L14 21l-2-1.6L10 21l-2.5-1.6L5 21z" />
    <path d="M9 8h6M9 12h6" />
  </Icone>
);

export const IconeCaixa = (p: P) => (
  <Icone {...p}>
    <path d="M21 8 12 3 3 8v8l9 5 9-5z" />
    <path d="m3 8 9 5 9-5M12 13v8" />
  </Icone>
);

export const IconeCaminhao = (p: P) => (
  <Icone {...p}>
    <path d="M2 6h11v10H2zM13 9h4.5L21 12.5V16h-8" />
    <circle cx="6.5" cy="17.5" r="1.8" />
    <circle cx="17" cy="17.5" r="1.8" />
  </Icone>
);

export const IconeAlerta = (p: P) => (
  <Icone {...p}>
    <path d="M10.3 3.9 1.8 18.5A2 2 0 0 0 3.5 21.5h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
    <path d="M12 9v4M12 17h.01" />
  </Icone>
);

export const IconeGrafico = (p: P) => (
  <Icone {...p}>
    <path d="M3 3v18h18" />
    <path d="m7 15 4-4 3 3 6-6" />
  </Icone>
);

export const IconeDinheiro = (p: P) => (
  <Icone {...p}>
    <rect x="2" y="6" width="20" height="12" rx="2" />
    <circle cx="12" cy="12" r="2.5" />
    <path d="M6 10v4M18 10v4" />
  </Icone>
);

export const IconeCalendario = (p: P) => (
  <Icone {...p}>
    <rect x="3" y="4.5" width="18" height="16.5" rx="2" />
    <path d="M3 9.5h18M8 2.5v4M16 2.5v4" />
  </Icone>
);

export const IconeTicket = (p: P) => (
  <Icone {...p}>
    <path d="M3 8a2 2 0 0 0 2-2h14a2 2 0 0 0 2 2v8a2 2 0 0 0-2 2H5a2 2 0 0 0-2-2z" />
    <path d="M9 12h6" />
  </Icone>
);

export const IconeMenu = (p: P) => (
  <Icone {...p}>
    <path d="M4 6h16M4 12h16M4 18h16" />
  </Icone>
);

export const IconeVazio = (p: P) => (
  <Icone {...p}>
    <path d="M21 8 12 3 3 8v8l9 5 9-5z" />
    <path d="m3 8 9 5 9-5" />
  </Icone>
);

export const IconeDesconectado = (p: P) => (
  <Icone {...p}>
    <path d="M2 2l20 20" />
    <path d="M8.5 16.5a5 5 0 0 1 7 0M5 13a10 10 0 0 1 5.2-2.8M19 13a10 10 0 0 0-2.4-1.7M2 8.8a15 15 0 0 1 4.2-2.7M22 8.8A15 15 0 0 0 10.7 5" />
    <path d="M12 20h.01" />
  </Icone>
);

export const IconeAtualizar = (p: P) => (
  <Icone {...p}>
    <path d="M21 12a9 9 0 1 1-2.6-6.4L21 8" />
    <path d="M21 3v5h-5" />
  </Icone>
);

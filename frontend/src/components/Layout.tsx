import { useState, type ComponentType } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { api } from '../api/client';
import { useCarregar } from '../utils/useCarregar';
import {
  IconeAlerta,
  IconeCaixa,
  IconeCaminhao,
  IconeCarrinho,
  IconeGrafico,
  IconeMenu,
  IconePainel,
  IconeRecibo,
} from './Icones';

type ItemMenu = { para: string; rotulo: string; Icone: ComponentType<{ tamanho?: number }> };

const MENU: { secao: string; itens: ItemMenu[] }[] = [
  { secao: 'Geral', itens: [{ para: '/', rotulo: 'Dashboard', Icone: IconePainel }] },
  {
    secao: 'Vendas',
    itens: [
      { para: '/vendas/nova', rotulo: 'Nova venda', Icone: IconeCarrinho },
      { para: '/vendas', rotulo: 'Histórico de vendas', Icone: IconeRecibo },
    ],
  },
  {
    secao: 'Estoque',
    itens: [
      { para: '/produtos', rotulo: 'Produtos', Icone: IconeCaixa },
      { para: '/entradas', rotulo: 'Entrada de mercadorias', Icone: IconeCaminhao },
      { para: '/reposicoes', rotulo: 'Reposição', Icone: IconeAlerta },
    ],
  },
  { secao: 'Análise', itens: [{ para: '/relatorios', rotulo: 'Relatórios', Icone: IconeGrafico }] },
];

const VERSAO_FRONTEND = import.meta.env.VITE_APP_VERSION ?? 'v1';

export function Layout() {
  const [menuAberto, setMenuAberto] = useState(false);
  const { pathname } = useLocation();
  // Atualiza a cada 10s: mostra qual Pod da API respondeu (util na demonstracao do Kubernetes).
  const info = useCarregar(() => api.info(), [], 10000);
  const statusApi = info.dados ? 'online' : info.erro ? 'offline' : 'conectando';

  return (
    <div className={`app ${menuAberto ? 'menu-aberto' : ''}`}>
      <aside className="menu-lateral">
        <div className="marca">
          <span className="marca-icone">
            <IconeCaixa tamanho={20} />
          </span>
          <div className="marca-texto">
            <strong>Estoque &amp; Vendas</strong>
            <small>Pequeno comércio</small>
          </div>
        </div>

        <nav>
          {MENU.map((grupo) => (
            <div key={grupo.secao} className="menu-grupo">
              <span className="menu-secao">{grupo.secao}</span>
              {grupo.itens.map(({ para, rotulo, Icone }) => (
                <NavLink
                  key={para}
                  to={para}
                  end={para === '/' || para === '/vendas'}
                  onClick={() => setMenuAberto(false)}
                >
                  <Icone />
                  <span>{rotulo}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <footer className="rodape-menu">
          <div className="rodape-linha">
            <span>Frontend</span>
            <span className="versao">{VERSAO_FRONTEND}</span>
          </div>
          <div
            className="rodape-linha"
            title={info.dados ? `Pod da API que respondeu: ${info.dados.instancia}` : undefined}
          >
            <span className={`status-api status-${statusApi}`}>
              <span className="ponto" />
              {statusApi === 'online' ? 'API' : statusApi === 'offline' ? 'API indisponível' : 'Conectando...'}
            </span>
            {info.dados && <span className="versao">{info.dados.versao}</span>}
          </div>
          {info.dados && <div className="pod">{info.dados.instancia}</div>}
        </footer>
      </aside>

      <div className="conteudo">
        <div className="barra-topo">
          <button type="button" className="btn-icone" onClick={() => setMenuAberto((v) => !v)} aria-label="Abrir menu">
            <IconeMenu tamanho={22} />
          </button>
          <strong>Estoque &amp; Vendas</strong>
        </div>
        <main key={pathname}>
          <Outlet />
        </main>
      </div>
      <div className="menu-fundo" onClick={() => setMenuAberto(false)} />
    </div>
  );
}

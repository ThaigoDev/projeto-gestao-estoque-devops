import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { AvisosProvider } from './components/Avisos';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AvisosProvider>
        <App />
      </AvisosProvider>
    </BrowserRouter>
  </StrictMode>,
);

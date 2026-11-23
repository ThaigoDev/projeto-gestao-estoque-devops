import { useCallback, useEffect, useState } from 'react';
import { mensagemDeErro } from '../api/client';

/**
 * Carrega dados da API e controla os estados de carregando/erro.
 * `deps` funciona como no useEffect: quando muda, busca de novo.
 * `intervaloMs` (opcional) atualiza sozinho de tempos em tempos.
 */
export function useCarregar<T>(buscar: () => Promise<T>, deps: unknown[] = [], intervaloMs?: number) {
  const [dados, setDados] = useState<T>();
  const [erro, setErro] = useState<string>();
  const [carregando, setCarregando] = useState(true);
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    buscar()
      .then((resultado) => {
        if (!ativo) return;
        setDados(resultado);
        setErro(undefined);
      })
      .catch((e) => ativo && setErro(mensagemDeErro(e)))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, versao]);

  useEffect(() => {
    if (!intervaloMs) return;
    const timer = setInterval(() => setVersao((v) => v + 1), intervaloMs);
    return () => clearInterval(timer);
  }, [intervaloMs]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { dados, erro, carregando, recarregar };
}

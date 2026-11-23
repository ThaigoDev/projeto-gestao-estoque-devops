package br.com.estoque.worker.model;

import java.time.Instant;
import java.util.List;

/** Dados de uma execucao do Worker, devolvidos em GET /status. */
public record ExecucaoWorker(
        long numero,
        Instant dataExecucao,
        boolean sucesso,
        long duracaoMs,
        Integer produtosSemEstoque,
        Integer novasReposicoes,
        List<ProdutoParaRepor> produtosParaRepor,
        ResumoRelatorio resumo,
        String erro) {

    public static ExecucaoWorker sucesso(long numero, Instant data, long duracaoMs,
                                         ResultadoVerificacao verificacao, ResumoRelatorio resumo) {
        return new ExecucaoWorker(numero, data, true, duracaoMs,
                verificacao.produtosSemEstoque(), verificacao.novasReposicoes(),
                verificacao.produtosParaRepor(), resumo, null);
    }

    public static ExecucaoWorker falha(long numero, Instant data, long duracaoMs, String erro) {
        return new ExecucaoWorker(numero, data, false, duracaoMs, null, null, null, null, erro);
    }
}

package br.com.estoque.worker.model;

import java.time.Instant;
import java.util.List;

/** O que uma execucao da verificacao de estoque encontrou. */
public record ResultadoVerificacao(
        Instant dataVerificacao,
        int produtosSemEstoque,
        int novasReposicoes,
        List<ProdutoParaRepor> produtosParaRepor) {
}

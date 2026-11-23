package br.com.estoque.worker.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resumo geral do comercio, recalculado pelo Worker a cada minuto e gravado
 * na colecao "relatorios" (documento com _id "resumo-geral").
 */
public record ResumoRelatorio(
        long quantidadeVendas,
        BigDecimal faturamentoTotal,
        long quantidadeItensVendidos,
        BigDecimal ticketMedio,
        long totalProdutosAtivos,
        long produtosSemEstoque,
        long reposicoesPendentes,
        Instant atualizadoEm) {
}

package br.com.estoque.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Respostas dos endpoints de relatorio e dashboard. */
public final class Relatorios {

    private Relatorios() {
    }

    public record Faturamento(
            LocalDate inicio,
            LocalDate fim,
            BigDecimal faturamentoTotal,
            long quantidadeVendas,
            long quantidadeItensVendidos,
            BigDecimal ticketMedio,
            Map<String, BigDecimal> porFormaPagamento,
            List<FaturamentoDiario> porDia) {
    }

    public record FaturamentoDiario(
            LocalDate data,
            BigDecimal faturamento,
            long quantidadeVendas) {
    }

    public record ProdutoMaisVendido(
            String produtoId,
            String produtoNome,
            long quantidadeVendida,
            BigDecimal faturamento) {
    }

    public record Dashboard(
            long totalProdutos,
            long produtosSemEstoque,
            long produtosEstoqueBaixo,
            long reposicoesPendentes,
            BigDecimal faturamentoHoje,
            long vendasHoje,
            BigDecimal faturamentoMes,
            long vendasMes) {
    }
}

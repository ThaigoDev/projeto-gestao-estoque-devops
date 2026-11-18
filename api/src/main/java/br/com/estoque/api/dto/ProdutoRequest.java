package br.com.estoque.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro e edicao de produto.
 * estoqueInicial so e usado no cadastro (POST) e vira uma entrada de mercadoria.
 * No PUT ele e ignorado: estoque so muda por entradas e vendas.
 */
public record ProdutoRequest(
        @Size(max = 50) String codigo,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 500) String descricao,
        @Size(max = 60) String categoria,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal precoVenda,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal precoCusto,
        @Min(0) Integer estoqueMinimo,
        @Min(0) Integer estoqueInicial) {
}

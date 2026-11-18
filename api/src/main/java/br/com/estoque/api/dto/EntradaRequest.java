package br.com.estoque.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntradaRequest(
        @NotBlank String produtoId,
        @NotNull @Min(1) Integer quantidade,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal custoUnitario,
        @Size(max = 120) String fornecedor,
        @Size(max = 500) String observacao) {
}

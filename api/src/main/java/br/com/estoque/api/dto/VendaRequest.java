package br.com.estoque.api.dto;

import java.util.List;

import br.com.estoque.api.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O cliente envia apenas produto e quantidade; o preco vem sempre do cadastro do produto,
 * para que o frontend nao consiga alterar o valor da venda.
 */
public record VendaRequest(
        @NotEmpty @Valid List<Item> itens,
        @NotNull FormaPagamento formaPagamento,
        @Size(max = 120) String cliente) {

    public record Item(
            @NotBlank String produtoId,
            @NotNull @Min(1) Integer quantidade) {
    }
}

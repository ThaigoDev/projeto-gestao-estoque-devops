package br.com.estoque.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 20) String cnpj,
        @Size(max = 20) String telefone,
        @Email @Size(max = 120) String email) {
}

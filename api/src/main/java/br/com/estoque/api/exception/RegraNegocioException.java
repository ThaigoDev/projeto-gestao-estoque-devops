package br.com.estoque.api.exception;

/** Requisicao valida no formato, mas que viola uma regra do sistema (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

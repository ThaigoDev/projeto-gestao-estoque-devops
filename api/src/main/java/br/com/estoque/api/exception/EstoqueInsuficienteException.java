package br.com.estoque.api.exception;

public class EstoqueInsuficienteException extends RegraNegocioException {

    private final String produtoId;
    private final int disponivel;
    private final int solicitado;

    public EstoqueInsuficienteException(String produtoId, String produtoNome, int disponivel, int solicitado) {
        super("Estoque insuficiente para '" + produtoNome + "': disponivel " + disponivel + ", solicitado " + solicitado);
        this.produtoId = produtoId;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public String getProdutoId() { return produtoId; }
    public int getDisponivel() { return disponivel; }
    public int getSolicitado() { return solicitado; }
}

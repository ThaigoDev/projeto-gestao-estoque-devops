package br.com.estoque.api.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colecao "reposicoes" - CONTRATO COM O WORKER (Pessoa 3).
 *
 * Quem grava: o Worker, a cada minuto, insere um documento com status PENDENTE para cada
 * produto ativo com quantidadeEstoque == 0 que ainda nao tenha uma reposicao PENDENTE.
 *
 * Quem resolve: a API, ao registrar uma entrada de mercadoria, muda as reposicoes
 * PENDENTE daquele produto para RESOLVIDA e preenche dataResolucao.
 */
@Document(collection = "reposicoes")
public class Reposicao {

    public static final String PENDENTE = "PENDENTE";
    public static final String RESOLVIDA = "RESOLVIDA";

    @Id
    private String id;

    @Indexed
    private String produtoId;
    private String produtoNome;
    private int quantidadeAtual;

    @Indexed
    private String status;

    private Instant dataRegistro;
    private Instant dataResolucao;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }
    public int getQuantidadeAtual() { return quantidadeAtual; }
    public void setQuantidadeAtual(int quantidadeAtual) { this.quantidadeAtual = quantidadeAtual; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(Instant dataRegistro) { this.dataRegistro = dataRegistro; }
    public Instant getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(Instant dataResolucao) { this.dataResolucao = dataResolucao; }
}

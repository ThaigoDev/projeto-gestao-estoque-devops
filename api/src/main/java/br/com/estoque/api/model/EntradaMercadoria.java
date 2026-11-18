package br.com.estoque.api.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/** Colecao "entradas": historico de reposicoes de estoque. */
@Document(collection = "entradas")
public class EntradaMercadoria {

    @Id
    private String id;

    @Indexed
    private String produtoId;
    private String produtoNome;

    private int quantidade;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal custoUnitario;

    private String fornecedor;
    private String observacao;

    /** Estoque do produto logo apos a entrada. */
    private int estoqueResultante;

    @Indexed
    private Instant dataEntrada;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public int getEstoqueResultante() { return estoqueResultante; }
    public void setEstoqueResultante(int estoqueResultante) { this.estoqueResultante = estoqueResultante; }
    public Instant getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(Instant dataEntrada) { this.dataEntrada = dataEntrada; }
}

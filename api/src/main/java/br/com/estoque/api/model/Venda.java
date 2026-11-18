package br.com.estoque.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * Colecao "vendas". Os itens sao embutidos no documento e guardam nome e preco
 * do momento da venda, entao o historico nao muda se o produto for editado depois.
 */
@Document(collection = "vendas")
public class Venda {

    @Id
    private String id;

    private List<ItemVenda> itens = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorTotal;

    /** Soma das quantidades de todos os itens. */
    private int quantidadeItens;

    private FormaPagamento formaPagamento;
    private String cliente;

    @Indexed
    private Instant dataVenda;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public int getQuantidadeItens() { return quantidadeItens; }
    public void setQuantidadeItens(int quantidadeItens) { this.quantidadeItens = quantidadeItens; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public Instant getDataVenda() { return dataVenda; }
    public void setDataVenda(Instant dataVenda) { this.dataVenda = dataVenda; }
}

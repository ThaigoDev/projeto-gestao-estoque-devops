package br.com.estoque.api.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Colecao "fornecedores".
 * Pode ser cadastrado direto (POST /api/fornecedores) ou criado automaticamente
 * quando uma entrada de mercadoria informa um fornecedor pelo nome.
 */
@Document(collection = "fornecedores")
public class Fornecedor {

    @Id
    private String id;

    private String nome;

    /** Nome em minusculas e sem espacos extras: evita "Distribuidora X" e "distribuidora x" duplicados. */
    @Indexed(unique = true)
    private String nomeBusca;

    private String cnpj;
    private String telefone;
    private String email;

    private boolean ativo = true;

    @CreatedDate
    private Instant criadoEm;

    @LastModifiedDate
    private Instant atualizadoEm;

    public static String normalizarNome(String nome) {
        return nome.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeBusca() { return nomeBusca; }
    public void setNomeBusca(String nomeBusca) { this.nomeBusca = nomeBusca; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}

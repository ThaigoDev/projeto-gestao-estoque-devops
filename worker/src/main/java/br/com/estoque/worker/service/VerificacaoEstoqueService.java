package br.com.estoque.worker.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import br.com.estoque.worker.model.ProdutoParaRepor;
import br.com.estoque.worker.model.ResultadoVerificacao;

/**
 * Verificacao automatica do estoque, seguindo o contrato combinado com a API
 * (ver "Contrato com o Worker" no api/README.md):
 *
 * 1. Busca em "produtos" os ativos com quantidadeEstoque menor ou igual a zero.
 * 2. Para cada um, garante que exista UM documento em "reposicoes" com status PENDENTE.
 *    Se ja existir, so atualiza nome e quantidade; nao cria duplicado.
 *
 * Quem muda a reposicao para RESOLVIDA e a API, quando chega uma entrada de mercadoria.
 */
@Service
public class VerificacaoEstoqueService {

    public static final String COLECAO_PRODUTOS = "produtos";
    public static final String COLECAO_REPOSICOES = "reposicoes";
    public static final String STATUS_PENDENTE = "PENDENTE";

    private final MongoTemplate mongo;

    public VerificacaoEstoqueService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public ResultadoVerificacao verificar() {
        Instant agora = Instant.now();

        // "ativo != false" tambem pega documentos antigos que nao tenham o campo
        Query semEstoque = Query.query(Criteria.where("ativo").ne(false).and("quantidadeEstoque").lte(0));
        semEstoque.fields().include("_id", "nome", "quantidadeEstoque");
        List<Document> produtos = mongo.find(semEstoque, Document.class, COLECAO_PRODUTOS);

        List<ProdutoParaRepor> paraRepor = new ArrayList<>();
        int novas = 0;

        for (Document produto : produtos) {
            String produtoId = idComoString(produto.get("_id"));
            String nome = produto.getString("nome");
            int quantidade = produto.get("quantidadeEstoque") instanceof Number n ? n.intValue() : 0;

            // Upsert: atualiza a reposicao PENDENTE do produto ou cria uma nova se nao houver.
            // produtoId e status vem da propria consulta quando o documento e criado.
            Query pendente = Query.query(Criteria.where("produtoId").is(produtoId).and("status").is(STATUS_PENDENTE));
            Update update = new Update()
                    .set("produtoNome", nome)
                    .set("quantidadeAtual", quantidade)
                    .setOnInsert("dataRegistro", agora);

            UpdateResult resultado = mongo.upsert(pendente, update, COLECAO_REPOSICOES);
            boolean criada = resultado.getUpsertedId() != null;
            if (criada) {
                novas++;
            }
            paraRepor.add(new ProdutoParaRepor(produtoId, nome, quantidade, criada));
        }

        return new ResultadoVerificacao(agora, produtos.size(), novas, paraRepor);
    }

    /** O _id do produto e ObjectId no Mongo; em "reposicoes" ele e gravado como string (contrato com a API). */
    private static String idComoString(Object id) {
        return id instanceof ObjectId objectId ? objectId.toHexString() : String.valueOf(id);
    }
}

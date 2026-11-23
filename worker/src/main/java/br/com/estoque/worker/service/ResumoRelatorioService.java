package br.com.estoque.worker.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import br.com.estoque.worker.model.ProdutoParaRepor;
import br.com.estoque.worker.model.ResultadoVerificacao;
import br.com.estoque.worker.model.ResumoRelatorio;

/**
 * Prepara os dados dos relatorios: quantidade total de vendas e faturamento total do comercio.
 *
 * A cada execucao recalcula os totais a partir da colecao "vendas" e grava o resultado
 * na colecao "relatorios", sempre no mesmo documento (_id "resumo-geral").
 */
@Service
public class ResumoRelatorioService {

    public static final String COLECAO_VENDAS = "vendas";
    public static final String COLECAO_RELATORIOS = "relatorios";
    public static final String ID_RESUMO = "resumo-geral";

    private final MongoTemplate mongo;

    public ResumoRelatorioService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public ResumoRelatorio atualizar(ResultadoVerificacao verificacao) {
        // Soma feita no proprio MongoDB: { $group: { _id: null, count, sum(valorTotal), sum(quantidadeItens) } }
        Aggregation totaisDeVendas = newAggregation(
                group()
                        .count().as("quantidadeVendas")
                        .sum("valorTotal").as("faturamentoTotal")
                        .sum("quantidadeItens").as("quantidadeItensVendidos"));
        Document totais = mongo.aggregate(totaisDeVendas, COLECAO_VENDAS, Document.class).getUniqueMappedResult();

        long quantidadeVendas = totais == null ? 0 : comoLong(totais.get("quantidadeVendas"));
        BigDecimal faturamento = totais == null ? BigDecimal.ZERO : comoBigDecimal(totais.get("faturamentoTotal"));
        faturamento = faturamento.setScale(2, RoundingMode.HALF_UP);
        long itensVendidos = totais == null ? 0 : comoLong(totais.get("quantidadeItensVendidos"));
        BigDecimal ticketMedio = quantidadeVendas == 0
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(quantidadeVendas), 2, RoundingMode.HALF_UP);

        long produtosAtivos = mongo.count(
                Query.query(Criteria.where("ativo").ne(false)), VerificacaoEstoqueService.COLECAO_PRODUTOS);
        long reposicoesPendentes = mongo.count(
                Query.query(Criteria.where("status").is(VerificacaoEstoqueService.STATUS_PENDENTE)),
                VerificacaoEstoqueService.COLECAO_REPOSICOES);

        ResumoRelatorio resumo = new ResumoRelatorio(
                quantidadeVendas, faturamento, itensVendidos, ticketMedio,
                produtosAtivos, verificacao.produtosSemEstoque(), reposicoesPendentes,
                verificacao.dataVerificacao());

        salvar(resumo, verificacao.produtosParaRepor());
        return resumo;
    }

    private void salvar(ResumoRelatorio resumo, List<ProdutoParaRepor> paraRepor) {
        List<Document> listaReposicao = paraRepor.stream()
                .map(p -> new Document("produtoId", p.produtoId())
                        .append("produtoNome", p.produtoNome())
                        .append("quantidadeAtual", p.quantidadeAtual()))
                .toList();

        // Dinheiro em Decimal128 (mesmo padrao da API, para nao perder centavos)
        Document doc = new Document("_id", ID_RESUMO)
                .append("quantidadeVendas", resumo.quantidadeVendas())
                .append("faturamentoTotal", new Decimal128(resumo.faturamentoTotal()))
                .append("quantidadeItensVendidos", resumo.quantidadeItensVendidos())
                .append("ticketMedio", new Decimal128(resumo.ticketMedio()))
                .append("totalProdutosAtivos", resumo.totalProdutosAtivos())
                .append("produtosSemEstoque", resumo.produtosSemEstoque())
                .append("reposicoesPendentes", resumo.reposicoesPendentes())
                .append("produtosParaRepor", listaReposicao)
                .append("atualizadoEm", Date.from(resumo.atualizadoEm()));

        mongo.getCollection(COLECAO_RELATORIOS)
                .replaceOne(Filters.eq("_id", ID_RESUMO), doc, new ReplaceOptions().upsert(true));
    }

    private static long comoLong(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0;
    }

    private static BigDecimal comoBigDecimal(Object valor) {
        if (valor instanceof Decimal128 d) {
            return d.bigDecimalValue();
        }
        if (valor instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return BigDecimal.ZERO;
    }
}

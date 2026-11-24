package br.com.estoque.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.estoque.worker.job.VerificacaoEstoqueJob;
import br.com.estoque.worker.model.ResultadoVerificacao;
import br.com.estoque.worker.model.ResumoRelatorio;
import br.com.estoque.worker.service.ResumoRelatorioService;
import br.com.estoque.worker.service.VerificacaoEstoqueService;

/** Worker contra um MongoDB real (embutido), com documentos no mesmo formato que a API grava. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WorkerIntegrationTest {

    @Autowired
    MongoTemplate mongo;
    @Autowired
    VerificacaoEstoqueService verificacao;
    @Autowired
    ResumoRelatorioService resumo;
    @Autowired
    VerificacaoEstoqueJob job;
    @Autowired
    MockMvc mvc;

    @BeforeEach
    void limpar() {
        for (String c : new String[] {"produtos", "vendas", "reposicao", "relatorios"}) {
            mongo.getCollection(c).deleteMany(new Document());
        }
    }

    @Test
    void registraReposicaoSomenteParaProdutosAtivosSemEstoque() {
        ObjectId arroz = produto("Arroz 5kg", 0, true);
        ObjectId oleo = produto("Oleo 900ml", 0, true);
        produto("Feijao 1kg", 8, true);
        produto("Leite 1L", 0, false); // desativado: nao entra

        ResultadoVerificacao r = verificacao.verificar();

        assertThat(r.produtosSemEstoque()).isEqualTo(2);
        assertThat(r.novasReposicoes()).isEqualTo(2);

        List<Document> reposicoes = mongo.getCollection("reposicao").find().into(new java.util.ArrayList<>());
        assertThat(reposicoes).extracting(d -> d.getString("produtoId"))
                .containsExactlyInAnyOrder(arroz.toHexString(), oleo.toHexString());

        Document doc = reposicoes.stream().filter(d -> d.getString("produtoId").equals(arroz.toHexString()))
                .findFirst().orElseThrow();
        assertThat(doc.getString("produtoNome")).isEqualTo("Arroz 5kg");
        assertThat(doc.getString("status")).isEqualTo("PENDENTE");
        assertThat(doc.getInteger("quantidadeAtual")).isZero();
        assertThat(doc.get("dataRegistro")).isInstanceOf(Date.class);
    }

    @Test
    void naoDuplicaReposicaoPendente() {
        produto("Arroz 5kg", 0, true);

        verificacao.verificar();
        ResultadoVerificacao segunda = verificacao.verificar();

        assertThat(segunda.produtosSemEstoque()).isEqualTo(1);
        assertThat(segunda.novasReposicoes()).isZero();
        assertThat(mongo.getCollection("reposicao").countDocuments()).isEqualTo(1);
    }

    @Test
    void criaNovaReposicaoQuandoProdutoZeraDeNovoDepoisDeResolvida() {
        ObjectId arroz = produto("Arroz 5kg", 0, true);
        verificacao.verificar();

        // A API resolve a reposicao quando chega mercadoria; depois o produto vende tudo de novo
        mongo.getCollection("reposicao").updateMany(
                new Document("produtoId", arroz.toHexString()),
                new Document("$set", new Document("status", "RESOLVIDA").append("dataResolucao", new Date())));

        ResultadoVerificacao r = verificacao.verificar();

        assertThat(r.novasReposicoes()).isEqualTo(1);
        assertThat(mongo.getCollection("reposicao").countDocuments()).isEqualTo(2);
        assertThat(mongo.getCollection("reposicao").countDocuments(new Document("status", "PENDENTE"))).isEqualTo(1);
    }

    @Test
    void calculaResumoDosRelatoriosEGravaNaColecaoRelatorios() {
        produto("Arroz 5kg", 0, true);
        produto("Feijao 1kg", 5, true);
        venda("81.15", 5);
        venda("18.85", 2);

        ResumoRelatorio r = resumo.atualizar(verificacao.verificar());

        assertThat(r.quantidadeVendas()).isEqualTo(2);
        assertThat(r.faturamentoTotal()).isEqualByComparingTo("100.00");
        assertThat(r.quantidadeItensVendidos()).isEqualTo(7);
        assertThat(r.ticketMedio()).isEqualByComparingTo("50.00");
        assertThat(r.totalProdutosAtivos()).isEqualTo(2);
        assertThat(r.produtosSemEstoque()).isEqualTo(1);
        assertThat(r.reposicoesPendentes()).isEqualTo(1);

        Document doc = mongo.getCollection("relatorios").find(new Document("_id", "resumo-geral")).first();
        assertThat(doc).isNotNull();
        assertThat(doc.get("faturamentoTotal")).isEqualTo(new Decimal128(new BigDecimal("100.00")));
        assertThat(doc.getLong("quantidadeVendas")).isEqualTo(2L);
        assertThat(doc.getList("produtosParaRepor", Document.class))
                .extracting(d -> d.getString("produtoNome")).containsExactly("Arroz 5kg");
    }

    @Test
    void resumoSemVendasFicaZerado() {
        ResumoRelatorio r = resumo.atualizar(verificacao.verificar());

        assertThat(r.quantidadeVendas()).isZero();
        assertThat(r.faturamentoTotal()).isEqualByComparingTo("0");
        assertThat(r.ticketMedio()).isEqualByComparingTo("0");
        assertThat(mongo.getCollection("relatorios").countDocuments()).isEqualTo(1);
    }

    @Test
    void execucaoAgendadaApareceNoStatus() throws Exception {
        produto("Arroz 5kg", 0, true);
        venda("10.00", 1);

        job.executar();

        mvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaExecucao.sucesso").value(true))
                .andExpect(jsonPath("$.ultimaExecucao.produtosSemEstoque").value(1))
                .andExpect(jsonPath("$.ultimaExecucao.resumo.quantidadeVendas").value(1))
                .andExpect(jsonPath("$.ultimaExecucao.resumo.faturamentoTotal").value(10.00));
    }

    // ---- dados no mesmo formato que a API grava ----

    private ObjectId produto(String nome, int quantidade, boolean ativo) {
        ObjectId id = new ObjectId();
        mongo.getCollection("produtos").insertOne(new Document("_id", id)
                .append("nome", nome)
                .append("precoVenda", new Decimal128(new BigDecimal("10.00")))
                .append("quantidadeEstoque", quantidade)
                .append("estoqueMinimo", 2)
                .append("ativo", ativo));
        return id;
    }

    private void venda(String valorTotal, int quantidadeItens) {
        mongo.getCollection("vendas").insertOne(new Document()
                .append("valorTotal", new Decimal128(new BigDecimal(valorTotal)))
                .append("quantidadeItens", quantidadeItens)
                .append("formaPagamento", "PIX")
                .append("dataVenda", new Date()));
    }
}

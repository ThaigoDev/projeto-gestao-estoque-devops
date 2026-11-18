package br.com.estoque.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Fluxo completo da API contra um MongoDB real (embutido). */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    MongoTemplate mongo;
    @Autowired
    ObjectMapper json;

    @BeforeEach
    void limpar() {
        for (String c : new String[] {"produtos", "entradas", "vendas", "reposicoes"}) {
            mongo.getCollection(c).deleteMany(new Document());
        }
    }

    @Test
    void fluxoCompletoDeEstoqueEVendas() throws Exception {
        String arroz = criarProduto("ARZ", "Arroz 5kg", "27.90", 3);
        String feijao = criarProduto("FJ", "Feijao 1kg", "8.45", 10);

        // preco gravado como Decimal128, nao como string/double
        Document doc = mongo.getCollection("produtos").find(new Document("nome", "Arroz 5kg")).first();
        assertThat(doc.get("precoVenda")).isInstanceOf(Decimal128.class);

        // venda valida: 2 arroz + 3 feijao = 55.80 + 25.35 = 81.15
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"produtoId":"%s","quantidade":2},{"produtoId":"%s","quantidade":3}],
                 "formaPagamento":"PIX"}""".formatted(arroz, feijao)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(81.15))
                .andExpect(jsonPath("$.quantidadeItens").value(5));

        // sem estoque: arroz tem 1, pede 2 -> 422 e nada e baixado (nem o feijao)
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"produtoId":"%s","quantidade":1},{"produtoId":"%s","quantidade":2}],
                 "formaPagamento":"DINHEIRO"}""".formatted(feijao, arroz)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes.disponivel").value(1));
        mvc.perform(get("/api/produtos/" + feijao)).andExpect(jsonPath("$.quantidadeEstoque").value(7));

        // zera o arroz
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"produtoId":"%s","quantidade":1}],"formaPagamento":"CARTAO_DEBITO"}""".formatted(arroz)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/produtos?estoque=zerado"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].semEstoque").value(true));

        // simula o Worker registrando reposicao
        mongo.getCollection("reposicoes").insertOne(new Document("produtoId", arroz)
                .append("produtoNome", "Arroz 5kg").append("quantidadeAtual", 0)
                .append("status", "PENDENTE").append("dataRegistro", java.util.Date.from(Instant.now())));
        mvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.produtosSemEstoque").value(1))
                .andExpect(jsonPath("$.reposicoesPendentes").value(1))
                .andExpect(jsonPath("$.vendasHoje").value(2))
                .andExpect(jsonPath("$.faturamentoHoje").value(109.05));

        // entrada repoe estoque e resolve a reposicao
        mvc.perform(post("/api/entradas").contentType(MediaType.APPLICATION_JSON).content("""
                {"produtoId":"%s","quantidade":10,"fornecedor":"Distribuidora X"}""".formatted(arroz)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estoqueResultante").value(10));
        mvc.perform(get("/api/reposicoes")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/reposicoes?status=RESOLVIDA")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/produtos/" + arroz + "/entradas")).andExpect(jsonPath("$.length()").value(2));

        // relatorios
        mvc.perform(get("/api/relatorios/faturamento"))
                .andExpect(jsonPath("$.faturamentoTotal").value(109.05))
                .andExpect(jsonPath("$.quantidadeVendas").value(2))
                .andExpect(jsonPath("$.ticketMedio").value(54.53))
                .andExpect(jsonPath("$.porDia.length()").value(30))
                .andExpect(jsonPath("$.porFormaPagamento.PIX").value(81.15));
        mvc.perform(get("/api/relatorios/produtos-mais-vendidos"))
                .andExpect(jsonPath("$[0].produtoNome").value("Arroz 5kg"))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(3));
        mvc.perform(get("/api/vendas")).andExpect(jsonPath("$.length()").value(2));

        // edicao nao mexe no estoque
        mvc.perform(put("/api/produtos/" + arroz).contentType(MediaType.APPLICATION_JSON).content("""
                {"codigo":"ARZ","nome":"Arroz Tipo 1","precoVenda":29.90,"estoqueMinimo":2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Arroz Tipo 1"))
                .andExpect(jsonPath("$.precoVenda").value(29.90))
                .andExpect(jsonPath("$.quantidadeEstoque").value(10));
        assertThat(mongo.getCollection("produtos").find(new Document("codigo", "ARZ")).first()
                .get("precoVenda")).isInstanceOf(Decimal128.class);

        // desativar
        mvc.perform(delete("/api/produtos/" + feijao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/produtos/" + feijao)).andExpect(status().isNotFound());
        mvc.perform(get("/api/produtos")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validacoesEErros() throws Exception {
        criarProduto("DUP", "Produto A", "1.00", 0);

        mvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"DUP\",\"nome\":\"Produto B\",\"precoVenda\":2}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"precoVenda\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.nome").exists())
                .andExpect(jsonPath("$.detalhes.precoVenda").exists());
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[],\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"produtoId\":\"x\",\"quantidade\":1}],\"formaPagamento\":\"BOLETO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"produtoId\":\"naoexiste\",\"quantidade\":1}],\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/relatorios/faturamento?inicio=2026-12-01&fim=2026-11-01"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/info")).andExpect(jsonPath("$.versao").value("v1"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    private String criarProduto(String codigo, String nome, String preco, int estoque) throws Exception {
        String body = mvc.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"codigo":"%s","nome":"%s","precoVenda":%s,"estoqueInicial":%d}"""
                        .formatted(codigo, nome, preco, estoque)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeEstoque").value(estoque))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asText();
    }
}

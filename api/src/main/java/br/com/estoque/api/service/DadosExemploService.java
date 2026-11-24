package br.com.estoque.api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.EntradaRequest;
import br.com.estoque.api.dto.FornecedorRequest;
import br.com.estoque.api.dto.ProdutoRequest;
import br.com.estoque.api.dto.VendaRequest;
import br.com.estoque.api.exception.RegraNegocioException;
import br.com.estoque.api.model.EntradaMercadoria;
import br.com.estoque.api.model.FormaPagamento;
import br.com.estoque.api.model.Fornecedor;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.model.Venda;
import br.com.estoque.api.repository.ProdutoRepository;

/**
 * Carga de dados ficticios para a demonstracao (requisito R3) de uma padaria/mercadinho.
 *
 * Tudo passa pelos mesmos services usados pelas telas, entao as regras valem aqui tambem
 * (estoque nunca negativo, total calculado pela API). Depois, as datas das entradas e vendas
 * sao espalhadas pelos ultimos dias para que os graficos e relatorios tenham o que mostrar.
 * Dois produtos terminam com estoque zero para o Worker registrar reposicao no minuto seguinte.
 */
@Service
public class DadosExemploService {

    private static final List<String> COLECOES = List.of("produtos", "fornecedores", "entradas", "vendas", "reposicao");
    private static final int DIAS_DE_HISTORICO = 14;

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;
    private final FornecedorService fornecedorService;
    private final EstoqueService estoqueService;
    private final VendaService vendaService;
    private final Clock clock;

    public DadosExemploService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository,
                               ProdutoService produtoService, FornecedorService fornecedorService,
                               EstoqueService estoqueService, VendaService vendaService, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
        this.produtoService = produtoService;
        this.fornecedorService = fornecedorService;
        this.estoqueService = estoqueService;
        this.vendaService = vendaService;
        this.clock = clock;
    }

    private record ProdutoExemplo(String codigo, String nome, String categoria, String preco, String custo,
                                  int minimo, int entrada, int fornecedor) {
    }

    private static final List<ProdutoExemplo> PRODUTOS = List.of(
            new ProdutoExemplo("PAO-FRANCES", "Pao frances (unidade)", "Padaria", "0.90", "0.35", 50, 400, 0),
            new ProdutoExemplo("PAO-FORMA", "Pao de forma 500g", "Padaria", "8.90", "5.20", 5, 30, 0),
            new ProdutoExemplo("BOLO-CENOURA", "Bolo de cenoura (fatia)", "Padaria", "6.50", "2.80", 5, 24, 0),
            new ProdutoExemplo("LEITE-1L", "Leite integral 1L", "Laticinios", "5.49", "3.90", 12, 60, 1),
            new ProdutoExemplo("QUEIJO-MUS", "Queijo mussarela 200g", "Laticinios", "12.90", "8.50", 5, 20, 1),
            new ProdutoExemplo("MANTEIGA", "Manteiga 200g", "Laticinios", "11.49", "7.80", 4, 15, 1),
            new ProdutoExemplo("CAFE-500", "Cafe torrado 500g", "Mercearia", "18.90", "13.00", 5, 25, 2),
            new ProdutoExemplo("ARROZ-5KG", "Arroz tipo 1 5kg", "Mercearia", "27.90", "19.50", 4, 18, 2),
            new ProdutoExemplo("FEIJAO-1KG", "Feijao carioca 1kg", "Mercearia", "8.45", "5.90", 6, 25, 2),
            new ProdutoExemplo("ACUCAR-1KG", "Acucar refinado 1kg", "Mercearia", "4.99", "3.40", 6, 8, 2),
            new ProdutoExemplo("REFRI-2L", "Refrigerante cola 2L", "Bebidas", "9.99", "6.50", 6, 30, 2),
            new ProdutoExemplo("SUCO-1L", "Suco de laranja 1L", "Bebidas", "7.90", "4.90", 4, 6, 1));

    /** Vendas fixas (indices em PRODUTOS) para o resultado ser sempre igual. */
    private static final int[][][] VENDAS = {
            {{0, 10}, {3, 2}}, {{1, 1}, {4, 1}}, {{6, 1}}, {{0, 6}, {2, 2}}, {{7, 1}, {8, 2}},
            {{10, 1}, {0, 8}}, {{9, 3}}, {{3, 3}, {5, 1}}, {{0, 12}, {11, 2}}, {{2, 3}},
            {{8, 1}, {9, 2}}, {{6, 1}, {4, 1}, {0, 4}}, {{11, 4}}, {{10, 2}, {1, 1}}, {{9, 3}},
            {{0, 20}, {3, 4}}, {{7, 1}}, {{2, 1}, {5, 1}}, {{3, 2}, {0, 5}}, {{10, 1}, {8, 1}},
    };

    public Map<String, Object> carregar(boolean limpar) {
        if (limpar) {
            COLECOES.forEach(c -> mongoTemplate.getCollection(c).deleteMany(new Document()));
        } else if (produtoRepository.count() > 0) {
            throw new RegraNegocioException(
                    "O banco ja tem produtos. Use ?limpar=true para apagar tudo e carregar os dados de exemplo");
        }

        List<Fornecedor> fornecedores = List.of(
                fornecedorService.criar(new FornecedorRequest("Moinho Bom Trigo", "12.345.678/0001-90", "(11) 3333-1001", "vendas@bomtrigo.example")),
                fornecedorService.criar(new FornecedorRequest("Laticinios Serra Verde", "23.456.789/0001-01", "(11) 3333-2002", "pedidos@serraverde.example")),
                fornecedorService.criar(new FornecedorRequest("Distribuidora Central", "34.567.890/0001-12", "(11) 3333-3003", "contato@distcentral.example")));

        List<Produto> produtos = new ArrayList<>();
        for (ProdutoExemplo p : PRODUTOS) {
            Produto produto = produtoService.criar(new ProdutoRequest(p.codigo(), p.nome(), null, p.categoria(),
                    new BigDecimal(p.preco()), new BigDecimal(p.custo()), p.minimo(), 0));
            estoqueService.registrarEntrada(new EntradaRequest(produto.getId(), p.entrada(), new BigDecimal(p.custo()),
                    null, "Carga de dados de exemplo", fornecedores.get(p.fornecedor()).getId()));
            produtos.add(produto);
        }

        Instant agora = Instant.now(clock);
        Instant inicio = agora.minus(Duration.ofDays(DIAS_DE_HISTORICO));
        mongoTemplate.updateMulti(new Query(), new Update().set("dataEntrada", inicio), EntradaMercadoria.class);

        // Datas das vendas: de 13 dias atras ate hoje, em horario comercial
        Random random = new Random(42);
        FormaPagamento[] formas = FormaPagamento.values();
        for (int i = 0; i < VENDAS.length; i++) {
            List<VendaRequest.Item> itens = new ArrayList<>();
            for (int[] item : VENDAS[i]) {
                itens.add(new VendaRequest.Item(produtos.get(item[0]).getId(), item[1]));
            }
            Venda venda = vendaService.registrar(new VendaRequest(itens, formas[i % formas.length], null));

            long diasAtras = (long) (DIAS_DE_HISTORICO - 1) * (VENDAS.length - 1 - i) / (VENDAS.length - 1);
            Instant data = agora.minus(Duration.ofDays(diasAtras))
                    .minus(Duration.ofMinutes(random.nextInt(8 * 60)));
            if (data.isAfter(agora)) {
                data = agora;
            }
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(venda.getId())),
                    new Update().set("dataVenda", data), Venda.class);
        }

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("fornecedores", fornecedores.size());
        resumo.put("produtos", produtos.size());
        resumo.put("entradas", produtos.size());
        resumo.put("vendas", VENDAS.length);
        resumo.put("produtosSemEstoque", produtoRepository.countByAtivoTrueAndQuantidadeEstoqueLessThanEqual(0));
        resumo.put("mensagem", "Dados de exemplo carregados. Em ate 1 minuto o Worker registra as reposicoes.");
        return resumo;
    }
}

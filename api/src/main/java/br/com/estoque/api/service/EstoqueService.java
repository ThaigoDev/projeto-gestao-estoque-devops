package br.com.estoque.api.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.EntradaRequest;
import br.com.estoque.api.exception.EstoqueInsuficienteException;
import br.com.estoque.api.exception.RecursoNaoEncontradoException;
import br.com.estoque.api.model.EntradaMercadoria;
import br.com.estoque.api.model.Fornecedor;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.model.Reposicao;
import br.com.estoque.api.repository.EntradaMercadoriaRepository;
import br.com.estoque.api.repository.ProdutoRepository;

/**
 * Unico lugar que altera quantidadeEstoque.
 *
 * As alteracoes usam findAndModify com $inc, que e atomico no MongoDB: se dois pedidos
 * tentarem vender a ultima unidade ao mesmo tempo, apenas um consegue
 * (a condicao "quantidadeEstoque >= quantidade" e verificada junto com a atualizacao).
 */
@Service
public class EstoqueService {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "dataEntrada");

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;
    private final EntradaMercadoriaRepository entradaRepository;
    private final FornecedorService fornecedorService;
    private final Clock clock;

    public EstoqueService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository,
                          EntradaMercadoriaRepository entradaRepository, FornecedorService fornecedorService,
                          Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
        this.entradaRepository = entradaRepository;
        this.fornecedorService = fornecedorService;
        this.clock = clock;
    }

    public EntradaMercadoria registrarEntrada(EntradaRequest req) {
        Instant agora = Instant.now(clock);

        // Resolve o fornecedor antes de mexer no estoque: um fornecedorId invalido nao altera nada
        if (!produtoRepository.existsByIdAndAtivoTrue(req.produtoId())) {
            throw new RecursoNaoEncontradoException("Produto nao encontrado: " + req.produtoId());
        }
        Fornecedor fornecedor = fornecedorService.resolver(req.fornecedorId(), req.fornecedor());

        Query query = Query.query(Criteria.where("_id").is(req.produtoId()).and("ativo").is(true));
        Update update = new Update()
                .inc("quantidadeEstoque", req.quantidade())
                .set("atualizadoEm", agora);
        Produto produto = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Produto.class);
        if (produto == null) {
            throw new RecursoNaoEncontradoException("Produto nao encontrado: " + req.produtoId());
        }

        EntradaMercadoria entrada = new EntradaMercadoria();
        entrada.setProdutoId(produto.getId());
        entrada.setProdutoNome(produto.getNome());
        entrada.setQuantidade(req.quantidade());
        entrada.setCustoUnitario(req.custoUnitario());
        if (fornecedor != null) {
            entrada.setFornecedorId(fornecedor.getId());
            entrada.setFornecedor(fornecedor.getNome());
        }
        entrada.setObservacao(req.observacao());
        entrada.setEstoqueResultante(produto.getQuantidadeEstoque());
        entrada.setDataEntrada(agora);
        EntradaMercadoria salva = entradaRepository.save(entrada);

        resolverReposicoesPendentes(produto.getId(), agora);
        return salva;
    }

    public List<EntradaMercadoria> listarEntradas(String produtoId) {
        if (produtoId == null || produtoId.isBlank()) {
            return entradaRepository.findAll(MAIS_RECENTES);
        }
        return entradaRepository.findByProdutoId(produtoId, MAIS_RECENTES);
    }

    /**
     * Retira a quantidade do estoque de forma atomica.
     * @return o produto ja com o estoque atualizado
     */
    public Produto baixar(String produtoId, int quantidade) {
        Query query = Query.query(Criteria.where("_id").is(produtoId)
                .and("ativo").is(true)
                .and("quantidadeEstoque").gte(quantidade));
        Update update = new Update()
                .inc("quantidadeEstoque", -quantidade)
                .set("atualizadoEm", Instant.now(clock));
        Produto produto = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Produto.class);
        if (produto != null) {
            return produto;
        }

        // Nao atualizou: descobre o motivo para devolver a mensagem certa
        Produto atual = produtoRepository.findById(produtoId)
                .filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado: " + produtoId));
        throw new EstoqueInsuficienteException(atual.getId(), atual.getNome(), atual.getQuantidadeEstoque(), quantidade);
    }

    /** Desfaz uma baixa (usado quando uma venda com varios itens falha no meio). */
    public void devolver(String produtoId, int quantidade) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(produtoId)),
                new Update().inc("quantidadeEstoque", quantidade).set("atualizadoEm", Instant.now(clock)),
                Produto.class);
    }

    private void resolverReposicoesPendentes(String produtoId, Instant agora) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("produtoId").is(produtoId).and("status").is(Reposicao.PENDENTE)),
                new Update().set("status", Reposicao.RESOLVIDA).set("dataResolucao", agora),
                Reposicao.class);
    }
}

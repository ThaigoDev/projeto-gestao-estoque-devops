package br.com.estoque.api.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.EntradaRequest;
import br.com.estoque.api.dto.ProdutoRequest;
import br.com.estoque.api.exception.RecursoNaoEncontradoException;
import br.com.estoque.api.exception.RegraNegocioException;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private static final Sort POR_NOME = Sort.by("nome");

    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public ProdutoService(ProdutoRepository produtoRepository, EstoqueService estoqueService,
                          MongoTemplate mongoTemplate, Clock clock) {
        this.produtoRepository = produtoRepository;
        this.estoqueService = estoqueService;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    /**
     * @param estoque "zerado" = sem estoque, "baixo" = abaixo ou igual ao estoque minimo
     */
    public List<Produto> listar(String nome, String categoria, String estoque) {
        if (estoque != null && !estoque.isBlank()) {
            return switch (estoque.toLowerCase()) {
                case "zerado" -> produtoRepository.findByAtivoTrueAndQuantidadeEstoqueLessThanEqual(0, POR_NOME);
                case "baixo" -> produtoRepository.findEstoqueBaixo(POR_NOME);
                default -> throw new RegraNegocioException("Filtro de estoque invalido: use 'zerado' ou 'baixo'");
            };
        }
        if (nome != null && !nome.isBlank()) {
            return produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nome.trim(), POR_NOME);
        }
        if (categoria != null && !categoria.isBlank()) {
            return produtoRepository.findByAtivoTrueAndCategoriaIgnoreCase(categoria.trim(), POR_NOME);
        }
        return produtoRepository.findByAtivoTrue(POR_NOME);
    }

    public Produto buscar(String id) {
        return produtoRepository.findById(id)
                .filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado: " + id));
    }

    public Produto criar(ProdutoRequest req) {
        String codigo = normalizar(req.codigo());
        if (codigo != null && produtoRepository.existsByCodigo(codigo)) {
            throw new DuplicateKeyException("Ja existe um produto com este codigo");
        }

        Produto produto = new Produto();
        produto.setCodigo(codigo);
        produto.setNome(req.nome().trim());
        produto.setDescricao(normalizar(req.descricao()));
        produto.setCategoria(normalizar(req.categoria()));
        produto.setPrecoVenda(req.precoVenda());
        produto.setPrecoCusto(req.precoCusto());
        produto.setEstoqueMinimo(req.estoqueMinimo() == null ? 0 : req.estoqueMinimo());
        produto.setQuantidadeEstoque(0);
        produto.setAtivo(true);
        produto = produtoRepository.save(produto);

        // O estoque inicial entra pelo mesmo caminho de uma entrada normal, para ficar no historico
        if (req.estoqueInicial() != null && req.estoqueInicial() > 0) {
            estoqueService.registrarEntrada(new EntradaRequest(
                    produto.getId(), req.estoqueInicial(), req.precoCusto(), null, "Estoque inicial"));
            produto = buscar(produto.getId());
        }
        return produto;
    }

    /** Atualiza apenas os dados cadastrais; quantidadeEstoque nao e tocada. */
    public Produto atualizar(String id, ProdutoRequest req) {
        String codigo = normalizar(req.codigo());
        Update update = new Update()
                .set("nome", req.nome().trim())
                .set("precoVenda", req.precoVenda())
                .set("estoqueMinimo", req.estoqueMinimo() == null ? 0 : req.estoqueMinimo())
                .set("atualizadoEm", Instant.now(clock));
        setOuRemover(update, "codigo", codigo);
        setOuRemover(update, "descricao", normalizar(req.descricao()));
        setOuRemover(update, "categoria", normalizar(req.categoria()));
        // $unset em campo Decimal128 falha no Spring Data (ele tenta converter o "1" do unset); grava null
        update.set("precoCusto", req.precoCusto());

        Produto atualizado = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("ativo").is(true)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Produto.class);
        if (atualizado == null) {
            throw new RecursoNaoEncontradoException("Produto nao encontrado: " + id);
        }
        return atualizado;
    }

    /** Exclusao logica: o produto some das listagens, mas continua no historico de vendas. */
    public void desativar(String id) {
        var resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id).and("ativo").is(true)),
                new Update().set("ativo", false).set("atualizadoEm", Instant.now(clock)),
                Produto.class);
        if (resultado.getMatchedCount() == 0) {
            throw new RecursoNaoEncontradoException("Produto nao encontrado: " + id);
        }
    }

    private static void setOuRemover(Update update, String campo, Object valor) {
        if (valor == null) {
            update.unset(campo);
        } else {
            update.set(campo, valor);
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

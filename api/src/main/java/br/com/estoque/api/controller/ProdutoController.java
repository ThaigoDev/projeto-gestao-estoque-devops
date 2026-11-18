package br.com.estoque.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.dto.ProdutoRequest;
import br.com.estoque.api.model.EntradaMercadoria;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.service.EstoqueService;
import br.com.estoque.api.service.ProdutoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public ProdutoController(ProdutoService produtoService, EstoqueService estoqueService) {
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    /** GET /api/produtos?nome=arroz | ?categoria=bebidas | ?estoque=zerado | ?estoque=baixo */
    @GetMapping
    public List<Produto> listar(@RequestParam(required = false) String nome,
                                @RequestParam(required = false) String categoria,
                                @RequestParam(required = false) String estoque) {
        return produtoService.listar(nome, categoria, estoque);
    }

    @GetMapping("/{id}")
    public Produto buscar(@PathVariable String id) {
        return produtoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@Valid @RequestBody ProdutoRequest req) {
        Produto produto = produtoService.criar(req);
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.getId())).body(produto);
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable String id, @Valid @RequestBody ProdutoRequest req) {
        return produtoService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        produtoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /** Historico de entradas de um produto. */
    @GetMapping("/{id}/entradas")
    public List<EntradaMercadoria> entradas(@PathVariable String id) {
        produtoService.buscar(id);
        return estoqueService.listarEntradas(id);
    }
}

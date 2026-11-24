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

import br.com.estoque.api.dto.FornecedorRequest;
import br.com.estoque.api.model.Fornecedor;
import br.com.estoque.api.service.FornecedorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping
    public List<Fornecedor> listar(@RequestParam(required = false) String nome) {
        return fornecedorService.listar(nome);
    }

    @GetMapping("/{id}")
    public Fornecedor buscar(@PathVariable String id) {
        return fornecedorService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Fornecedor> criar(@Valid @RequestBody FornecedorRequest req) {
        Fornecedor fornecedor = fornecedorService.criar(req);
        return ResponseEntity.created(URI.create("/api/fornecedores/" + fornecedor.getId())).body(fornecedor);
    }

    @PutMapping("/{id}")
    public Fornecedor atualizar(@PathVariable String id, @Valid @RequestBody FornecedorRequest req) {
        return fornecedorService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        fornecedorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}

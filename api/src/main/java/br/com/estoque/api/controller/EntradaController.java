package br.com.estoque.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.dto.EntradaRequest;
import br.com.estoque.api.model.EntradaMercadoria;
import br.com.estoque.api.service.EstoqueService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EstoqueService estoqueService;

    public EntradaController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<EntradaMercadoria> listar(@RequestParam(required = false) String produtoId) {
        return estoqueService.listarEntradas(produtoId);
    }

    @PostMapping
    public ResponseEntity<EntradaMercadoria> registrar(@Valid @RequestBody EntradaRequest req) {
        EntradaMercadoria entrada = estoqueService.registrarEntrada(req);
        return ResponseEntity.created(URI.create("/api/entradas/" + entrada.getId())).body(entrada);
    }
}

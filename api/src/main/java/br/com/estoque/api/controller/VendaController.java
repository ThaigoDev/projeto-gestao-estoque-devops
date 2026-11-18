package br.com.estoque.api.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.dto.VendaRequest;
import br.com.estoque.api.model.Venda;
import br.com.estoque.api.service.VendaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    /** Historico. GET /api/vendas?inicio=2026-11-01&fim=2026-11-30 (datas opcionais). */
    @GetMapping
    public List<Venda> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return vendaService.listar(inicio, fim);
    }

    @GetMapping("/{id}")
    public Venda buscar(@PathVariable String id) {
        return vendaService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Venda> registrar(@Valid @RequestBody VendaRequest req) {
        Venda venda = vendaService.registrar(req);
        return ResponseEntity.created(URI.create("/api/vendas/" + venda.getId())).body(venda);
    }
}

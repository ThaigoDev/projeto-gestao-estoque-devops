package br.com.estoque.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.dto.Relatorios;
import br.com.estoque.api.service.RelatorioService;

@RestController
@RequestMapping("/api")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/dashboard")
    public Relatorios.Dashboard dashboard() {
        return relatorioService.dashboard();
    }

    /** Sem datas: ultimos 30 dias. */
    @GetMapping("/relatorios/faturamento")
    public Relatorios.Faturamento faturamento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.faturamento(inicio, fim);
    }

    @GetMapping("/relatorios/produtos-mais-vendidos")
    public List<Relatorios.ProdutoMaisVendido> maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "10") int limite) {
        return relatorioService.produtosMaisVendidos(inicio, fim, limite);
    }
}

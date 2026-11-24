package br.com.estoque.api.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.service.DadosExemploService;

/**
 * POST /api/dados-exemplo              carrega os dados (so se o banco estiver sem produtos)
 * POST /api/dados-exemplo?limpar=true  apaga tudo e carrega de novo
 */
@RestController
public class DadosExemploController {

    private final DadosExemploService dadosExemploService;

    public DadosExemploController(DadosExemploService dadosExemploService) {
        this.dadosExemploService = dadosExemploService;
    }

    @PostMapping("/api/dados-exemplo")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> carregar(@RequestParam(defaultValue = "false") boolean limpar) {
        return dadosExemploService.carregar(limpar);
    }
}

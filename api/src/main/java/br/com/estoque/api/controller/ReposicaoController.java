package br.com.estoque.api.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.api.model.Reposicao;
import br.com.estoque.api.repository.ReposicaoRepository;

/** Leitura dos registros de reposicao gravados pelo Worker. */
@RestController
@RequestMapping("/api/reposicoes")
public class ReposicaoController {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "dataRegistro");

    private final ReposicaoRepository reposicaoRepository;

    public ReposicaoController(ReposicaoRepository reposicaoRepository) {
        this.reposicaoRepository = reposicaoRepository;
    }

    /** GET /api/reposicoes (pendentes) | ?status=RESOLVIDA | ?status=TODAS */
    @GetMapping
    public List<Reposicao> listar(@RequestParam(defaultValue = Reposicao.PENDENTE) String status) {
        if ("TODAS".equalsIgnoreCase(status)) {
            return reposicaoRepository.findAll(MAIS_RECENTES);
        }
        return reposicaoRepository.findByStatus(status.toUpperCase(), MAIS_RECENTES);
    }
}

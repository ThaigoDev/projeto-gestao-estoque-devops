package br.com.estoque.worker.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.estoque.worker.job.VerificacaoEstoqueJob;

/**
 * GET /status: mostra a ultima execucao do Worker.
 * Util na apresentacao para provar que ele esta rodando a cada minuto
 * (alem dos logs: docker compose logs -f worker / kubectl logs -f deploy/worker).
 */
@RestController
public class StatusController {

    private final VerificacaoEstoqueJob job;
    private final long intervaloMs;

    public StatusController(VerificacaoEstoqueJob job, @Value("${worker.intervalo-ms}") long intervaloMs) {
        this.job = job;
        this.intervaloMs = intervaloMs;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("servico", "estoque-worker");
        resposta.put("intervaloMs", intervaloMs);
        resposta.put("totalExecucoes", job.getTotalExecucoes());
        resposta.put("ultimaExecucao", job.getUltimaExecucao());
        return resposta;
    }
}

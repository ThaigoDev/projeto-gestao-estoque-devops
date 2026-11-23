package br.com.estoque.worker.job;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.estoque.worker.model.ExecucaoWorker;
import br.com.estoque.worker.model.ProdutoParaRepor;
import br.com.estoque.worker.model.ResultadoVerificacao;
import br.com.estoque.worker.model.ResumoRelatorio;
import br.com.estoque.worker.service.ResumoRelatorioService;
import br.com.estoque.worker.service.VerificacaoEstoqueService;

/**
 * Tarefa agendada: roda a cada minuto (worker.intervalo-ms).
 * Se o MongoDB estiver fora do ar, registra o erro no log e tenta de novo na proxima execucao.
 */
@Component
public class VerificacaoEstoqueJob {

    private static final Logger log = LoggerFactory.getLogger(VerificacaoEstoqueJob.class);
    private static final NumberFormat REAL = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final VerificacaoEstoqueService verificacaoService;
    private final ResumoRelatorioService resumoService;

    private final AtomicLong contador = new AtomicLong();
    private final AtomicReference<ExecucaoWorker> ultimaExecucao = new AtomicReference<>();

    public VerificacaoEstoqueJob(VerificacaoEstoqueService verificacaoService, ResumoRelatorioService resumoService) {
        this.verificacaoService = verificacaoService;
        this.resumoService = resumoService;
    }

    @Scheduled(fixedRateString = "${worker.intervalo-ms}", initialDelayString = "${worker.atraso-inicial-ms}")
    public void executar() {
        long numero = contador.incrementAndGet();
        Instant inicio = Instant.now();
        long t0 = System.currentTimeMillis();

        try {
            ResultadoVerificacao verificacao = verificacaoService.verificar();
            ResumoRelatorio resumo = resumoService.atualizar(verificacao);
            long duracao = System.currentTimeMillis() - t0;

            for (ProdutoParaRepor p : verificacao.produtosParaRepor()) {
                if (p.novaReposicao()) {
                    log.info("Reposicao registrada: {} (estoque {})", p.produtoNome(), p.quantidadeAtual());
                }
            }
            log.info("Verificacao #{}: {} produto(s) sem estoque, {} nova(s) reposicao(oes), {} pendente(s) | "
                            + "vendas: {}, faturamento total: {} ({} ms)",
                    numero, verificacao.produtosSemEstoque(), verificacao.novasReposicoes(),
                    resumo.reposicoesPendentes(), resumo.quantidadeVendas(),
                    REAL.format(resumo.faturamentoTotal()), duracao);

            ultimaExecucao.set(ExecucaoWorker.sucesso(numero, inicio, duracao, verificacao, resumo));
        } catch (RuntimeException e) {
            long duracao = System.currentTimeMillis() - t0;
            log.error("Verificacao #{} falhou (nova tentativa no proximo minuto): {}", numero, e.getMessage());
            ultimaExecucao.set(ExecucaoWorker.falha(numero, inicio, duracao, e.getMessage()));
        }
    }

    public ExecucaoWorker getUltimaExecucao() {
        return ultimaExecucao.get();
    }

    public long getTotalExecucoes() {
        return contador.get();
    }
}

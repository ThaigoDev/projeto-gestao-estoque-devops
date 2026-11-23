package br.com.estoque.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servico Worker (Pessoa 3).
 * Roda separado da API e, a cada minuto, verifica o estoque no MongoDB,
 * registra os produtos que precisam de reposicao e atualiza o resumo dos relatorios.
 */
@SpringBootApplication
@EnableScheduling
public class EstoqueWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstoqueWorkerApplication.class, args);
    }
}

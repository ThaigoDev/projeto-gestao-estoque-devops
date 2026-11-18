package br.com.estoque.api.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ajuda na demonstracao do Kubernetes:
 * - "versao" mostra se o Pod esta rodando a imagem v1 ou v2
 * - "instancia" e o nome do Pod (hostname), mostrando o balanceamento entre replicas
 */
@RestController
public class InfoController {

    @Value("${app.versao}")
    private String versao;

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "servico", "estoque-api",
                "versao", versao,
                "instancia", hostname());
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "desconhecido";
        }
    }
}

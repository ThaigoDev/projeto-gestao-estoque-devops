package br.com.estoque.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.VendaRequest;
import br.com.estoque.api.exception.RecursoNaoEncontradoException;
import br.com.estoque.api.model.ItemVenda;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.model.Venda;
import br.com.estoque.api.repository.VendaRepository;

@Service
public class VendaService {

    private static final Logger log = LoggerFactory.getLogger(VendaService.class);
    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "dataVenda");

    private final VendaRepository vendaRepository;
    private final EstoqueService estoqueService;
    private final Periodos periodos;
    private final Clock clock;

    public VendaService(VendaRepository vendaRepository, EstoqueService estoqueService,
                        Periodos periodos, Clock clock) {
        this.vendaRepository = vendaRepository;
        this.estoqueService = estoqueService;
        this.periodos = periodos;
        this.clock = clock;
    }

    /**
     * Registra a venda e baixa o estoque.
     *
     * Regra principal: nenhuma venda e registrada sem estoque suficiente.
     * Cada item e baixado de forma atomica; se algum item falhar, as baixas ja feitas
     * sao devolvidas (compensacao), entao a venda acontece por inteiro ou nao acontece.
     */
    public Venda registrar(VendaRequest req) {
        Map<String, Integer> quantidades = agruparPorProduto(req.itens());

        List<ItemVenda> itens = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> e : quantidades.entrySet()) {
                Produto produto = estoqueService.baixar(e.getKey(), e.getValue());
                BigDecimal preco = produto.getPrecoVenda();
                BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(e.getValue()))
                        .setScale(2, RoundingMode.HALF_UP);
                itens.add(new ItemVenda(produto.getId(), produto.getNome(), e.getValue(), preco, subtotal));
            }

            Venda venda = new Venda();
            venda.setItens(itens);
            venda.setValorTotal(itens.stream().map(ItemVenda::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP));
            venda.setQuantidadeItens(itens.stream().mapToInt(ItemVenda::getQuantidade).sum());
            venda.setFormaPagamento(req.formaPagamento());
            venda.setCliente(req.cliente() == null || req.cliente().isBlank() ? null : req.cliente().trim());
            venda.setDataVenda(Instant.now(clock));
            return vendaRepository.save(venda);
        } catch (RuntimeException ex) {
            desfazerBaixas(itens);
            throw ex;
        }
    }

    public List<Venda> listar(LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) {
            return vendaRepository.findAll(MAIS_RECENTES);
        }
        Periodos.Intervalo intervalo = periodos.resolver(inicio, fim);
        return vendaRepository.findNoPeriodo(intervalo.inicioInstant(), intervalo.fimInstant(), MAIS_RECENTES);
    }

    public Venda buscar(String id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Venda nao encontrada: " + id));
    }

    /** Soma itens repetidos do mesmo produto, para validar o estoque pelo total. */
    private static Map<String, Integer> agruparPorProduto(List<VendaRequest.Item> itens) {
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (VendaRequest.Item item : itens) {
            quantidades.merge(item.produtoId().trim(), item.quantidade(), Integer::sum);
        }
        return quantidades;
    }

    private void desfazerBaixas(List<ItemVenda> baixados) {
        for (ItemVenda item : baixados) {
            try {
                estoqueService.devolver(item.getProdutoId(), item.getQuantidade());
            } catch (RuntimeException ex) {
                log.error("Falha ao devolver {} unidade(s) do produto {} ao estoque",
                        item.getQuantidade(), item.getProdutoId(), ex);
            }
        }
    }
}

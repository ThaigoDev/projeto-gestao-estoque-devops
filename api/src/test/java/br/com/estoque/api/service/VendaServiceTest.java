package br.com.estoque.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.estoque.api.dto.VendaRequest;
import br.com.estoque.api.exception.EstoqueInsuficienteException;
import br.com.estoque.api.model.FormaPagamento;
import br.com.estoque.api.model.Produto;
import br.com.estoque.api.model.Venda;
import br.com.estoque.api.repository.VendaRepository;

class VendaServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private VendaRepository vendaRepository;
    private EstoqueService estoqueService;
    private VendaService service;

    @BeforeEach
    void setUp() {
        vendaRepository = mock(VendaRepository.class);
        estoqueService = mock(EstoqueService.class);
        service = new VendaService(vendaRepository, estoqueService, new Periodos(clock), clock);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void calculaTotalComPrecoDoCadastroEAgrupaItensRepetidos() {
        when(estoqueService.baixar("p1", 3)).thenReturn(produto("p1", "Arroz", "10.50"));
        when(estoqueService.baixar("p2", 1)).thenReturn(produto("p2", "Feijao", "7.99"));

        Venda venda = service.registrar(new VendaRequest(List.of(
                new VendaRequest.Item("p1", 2),
                new VendaRequest.Item("p2", 1),
                new VendaRequest.Item("p1", 1)), FormaPagamento.PIX, null));

        assertThat(venda.getItens()).hasSize(2);
        assertThat(venda.getItens().get(0).getSubtotal()).isEqualByComparingTo("31.50");
        assertThat(venda.getValorTotal()).isEqualByComparingTo("39.49");
        assertThat(venda.getQuantidadeItens()).isEqualTo(4);
        assertThat(venda.getDataVenda()).isEqualTo(clock.instant());
    }

    @Test
    void semEstoqueNaoRegistraVendaEDevolveOQueJaFoiBaixado() {
        when(estoqueService.baixar("p1", 2)).thenReturn(produto("p1", "Arroz", "10.50"));
        when(estoqueService.baixar("p2", 5))
                .thenThrow(new EstoqueInsuficienteException("p2", "Feijao", 1, 5));

        assertThatThrownBy(() -> service.registrar(new VendaRequest(List.of(
                new VendaRequest.Item("p1", 2),
                new VendaRequest.Item("p2", 5)), FormaPagamento.DINHEIRO, null)))
                .isInstanceOf(EstoqueInsuficienteException.class);

        verify(estoqueService).devolver("p1", 2);
        verify(vendaRepository, never()).save(any());
    }

    @Test
    void falhaAoSalvarVendaDevolveTodoOEstoque() {
        when(estoqueService.baixar("p1", 1)).thenReturn(produto("p1", "Arroz", "10.50"));
        when(vendaRepository.save(any(Venda.class))).thenThrow(new RuntimeException("Mongo fora"));

        assertThatThrownBy(() -> service.registrar(new VendaRequest(List.of(
                new VendaRequest.Item("p1", 1)), FormaPagamento.PIX, null)))
                .hasMessage("Mongo fora");

        verify(estoqueService).devolver("p1", 1);
    }

    private static Produto produto(String id, String nome, String preco) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPrecoVenda(new BigDecimal(preco));
        return p;
    }
}

package br.com.estoque.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.Relatorios;
import br.com.estoque.api.model.FormaPagamento;
import br.com.estoque.api.model.ItemVenda;
import br.com.estoque.api.model.Reposicao;
import br.com.estoque.api.model.Venda;
import br.com.estoque.api.repository.ProdutoRepository;
import br.com.estoque.api.repository.ReposicaoRepository;
import br.com.estoque.api.repository.VendaRepository;

@Service
public class RelatorioService {

    private static final Sort POR_DATA = Sort.by("dataVenda");

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ReposicaoRepository reposicaoRepository;
    private final Periodos periodos;

    public RelatorioService(VendaRepository vendaRepository, ProdutoRepository produtoRepository,
                            ReposicaoRepository reposicaoRepository, Periodos periodos) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.reposicaoRepository = reposicaoRepository;
        this.periodos = periodos;
    }

    /** Faturamento total, quantidade de vendas, ticket medio e serie diaria (para grafico). */
    public Relatorios.Faturamento faturamento(LocalDate inicio, LocalDate fim) {
        Periodos.Intervalo intervalo = periodos.resolver(inicio, fim);
        List<Venda> vendas = buscarVendas(intervalo);

        BigDecimal total = BigDecimal.ZERO;
        long itensVendidos = 0;
        Map<FormaPagamento, BigDecimal> porForma = new EnumMap<>(FormaPagamento.class);
        Map<LocalDate, BigDecimal> faturamentoDia = new TreeMap<>();
        Map<LocalDate, Long> vendasDia = new HashMap<>();

        for (LocalDate d = intervalo.inicio(); !d.isAfter(intervalo.fim()); d = d.plusDays(1)) {
            faturamentoDia.put(d, BigDecimal.ZERO);
            vendasDia.put(d, 0L);
        }

        for (Venda v : vendas) {
            total = total.add(v.getValorTotal());
            itensVendidos += v.getQuantidadeItens();
            if (v.getFormaPagamento() != null) {
                porForma.merge(v.getFormaPagamento(), v.getValorTotal(), BigDecimal::add);
            }
            LocalDate dia = v.getDataVenda().atZone(intervalo.zona()).toLocalDate();
            faturamentoDia.merge(dia, v.getValorTotal(), BigDecimal::add);
            vendasDia.merge(dia, 1L, Long::sum);
        }

        BigDecimal ticketMedio = vendas.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(vendas.size()), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> porFormaTexto = new LinkedHashMap<>();
        porForma.forEach((forma, valor) -> porFormaTexto.put(forma.name(), dinheiro(valor)));

        List<Relatorios.FaturamentoDiario> porDia = faturamentoDia.entrySet().stream()
                .map(e -> new Relatorios.FaturamentoDiario(e.getKey(), dinheiro(e.getValue()), vendasDia.get(e.getKey())))
                .toList();

        return new Relatorios.Faturamento(intervalo.inicio(), intervalo.fim(), dinheiro(total),
                vendas.size(), itensVendidos, ticketMedio, porFormaTexto, porDia);
    }

    public List<Relatorios.ProdutoMaisVendido> produtosMaisVendidos(LocalDate inicio, LocalDate fim, int limite) {
        Periodos.Intervalo intervalo = periodos.resolver(inicio, fim);

        Map<String, String> nomes = new HashMap<>();
        Map<String, Long> quantidades = new HashMap<>();
        Map<String, BigDecimal> valores = new HashMap<>();
        for (Venda v : buscarVendas(intervalo)) {
            for (ItemVenda item : v.getItens()) {
                nomes.put(item.getProdutoId(), item.getProdutoNome());
                quantidades.merge(item.getProdutoId(), (long) item.getQuantidade(), Long::sum);
                valores.merge(item.getProdutoId(), item.getSubtotal(), BigDecimal::add);
            }
        }

        return quantidades.entrySet().stream()
                .map(e -> new Relatorios.ProdutoMaisVendido(e.getKey(), nomes.get(e.getKey()),
                        e.getValue(), dinheiro(valores.get(e.getKey()))))
                .sorted(Comparator.comparingLong(Relatorios.ProdutoMaisVendido::quantidadeVendida).reversed()
                        .thenComparing(Relatorios.ProdutoMaisVendido::faturamento, Comparator.reverseOrder()))
                .limit(Math.max(1, Math.min(limite, 100)))
                .toList();
    }

    /** Numeros da tela inicial. */
    public Relatorios.Dashboard dashboard() {
        LocalDate hoje = periodos.hoje();
        List<Venda> vendasMes = buscarVendas(periodos.resolver(hoje.withDayOfMonth(1), hoje));

        BigDecimal faturamentoMes = BigDecimal.ZERO;
        BigDecimal faturamentoHoje = BigDecimal.ZERO;
        long vendasHoje = 0;
        for (Venda v : vendasMes) {
            faturamentoMes = faturamentoMes.add(v.getValorTotal());
            if (v.getDataVenda().atZone(periodos.zona()).toLocalDate().equals(hoje)) {
                faturamentoHoje = faturamentoHoje.add(v.getValorTotal());
                vendasHoje++;
            }
        }

        return new Relatorios.Dashboard(
                produtoRepository.countByAtivoTrue(),
                produtoRepository.countByAtivoTrueAndQuantidadeEstoqueLessThanEqual(0),
                produtoRepository.countEstoqueBaixo(),
                reposicaoRepository.countByStatus(Reposicao.PENDENTE),
                dinheiro(faturamentoHoje),
                vendasHoje,
                dinheiro(faturamentoMes),
                vendasMes.size());
    }

    private List<Venda> buscarVendas(Periodos.Intervalo intervalo) {
        return vendaRepository.findNoPeriodo(intervalo.inicioInstant(), intervalo.fimInstant(), POR_DATA);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}

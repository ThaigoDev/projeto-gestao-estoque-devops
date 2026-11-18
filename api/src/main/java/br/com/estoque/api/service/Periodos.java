package br.com.estoque.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import br.com.estoque.api.exception.RegraNegocioException;

/** Converte datas do filtro (dia local) em instantes para consultar o MongoDB. */
@Component
public class Periodos {

    static final int DIAS_PADRAO = 30;
    static final int DIAS_MAXIMO = 366;

    private final Clock clock;

    public Periodos(Clock clock) {
        this.clock = clock;
    }

    public record Intervalo(LocalDate inicio, LocalDate fim, ZoneId zona) {
        /** Inicio do primeiro dia (inclusivo). */
        public Instant inicioInstant() {
            return inicio.atStartOfDay(zona).toInstant();
        }

        /** Inicio do dia seguinte ao ultimo (exclusivo). */
        public Instant fimInstant() {
            return fim.plusDays(1).atStartOfDay(zona).toInstant();
        }
    }

    /** Sem datas: ultimos 30 dias ate hoje. */
    public Intervalo resolver(LocalDate inicio, LocalDate fim) {
        LocalDate fimEfetivo = fim != null ? fim : hoje();
        LocalDate inicioEfetivo = inicio != null ? inicio : fimEfetivo.minusDays(DIAS_PADRAO - 1L);
        if (inicioEfetivo.isAfter(fimEfetivo)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual a data final");
        }
        if (ChronoUnit.DAYS.between(inicioEfetivo, fimEfetivo) >= DIAS_MAXIMO) {
            throw new RegraNegocioException("O periodo maximo do relatorio e de " + DIAS_MAXIMO + " dias");
        }
        return new Intervalo(inicioEfetivo, fimEfetivo, clock.getZone());
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    public ZoneId zona() {
        return clock.getZone();
    }
}

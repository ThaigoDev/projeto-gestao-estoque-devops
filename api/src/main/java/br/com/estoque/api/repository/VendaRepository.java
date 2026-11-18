package br.com.estoque.api.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import br.com.estoque.api.model.Venda;

public interface VendaRepository extends MongoRepository<Venda, String> {

    /** Intervalo semiaberto: inicio <= dataVenda < fim. */
    @Query("{ 'dataVenda': { $gte: ?0, $lt: ?1 } }")
    List<Venda> findNoPeriodo(Instant inicio, Instant fim, Sort sort);
}

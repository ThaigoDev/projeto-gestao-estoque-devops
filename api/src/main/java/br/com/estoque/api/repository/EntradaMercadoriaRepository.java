package br.com.estoque.api.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.estoque.api.model.EntradaMercadoria;

public interface EntradaMercadoriaRepository extends MongoRepository<EntradaMercadoria, String> {

    List<EntradaMercadoria> findByProdutoId(String produtoId, Sort sort);
}

package br.com.estoque.api.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.estoque.api.model.Reposicao;

public interface ReposicaoRepository extends MongoRepository<Reposicao, String> {

    List<Reposicao> findByStatus(String status, Sort sort);

    long countByStatus(String status);
}

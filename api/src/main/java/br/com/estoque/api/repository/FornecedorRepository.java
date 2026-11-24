package br.com.estoque.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.estoque.api.model.Fornecedor;

public interface FornecedorRepository extends MongoRepository<Fornecedor, String> {

    List<Fornecedor> findByAtivoTrue(Sort sort);

    List<Fornecedor> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Sort sort);

    Optional<Fornecedor> findByNomeBusca(String nomeBusca);
}

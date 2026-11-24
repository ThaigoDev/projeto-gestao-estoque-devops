package br.com.estoque.api.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import br.com.estoque.api.model.Produto;

public interface ProdutoRepository extends MongoRepository<Produto, String> {

    List<Produto> findByAtivoTrue(Sort sort);

    List<Produto> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Sort sort);

    List<Produto> findByAtivoTrueAndCategoriaIgnoreCase(String categoria, Sort sort);

    List<Produto> findByAtivoTrueAndQuantidadeEstoqueLessThanEqual(int quantidade, Sort sort);

    /** Produtos cujo estoque esta igual ou abaixo do estoque minimo configurado. */
    @Query("{ 'ativo': true, $expr: { $lte: ['$quantidadeEstoque', '$estoqueMinimo'] } }")
    List<Produto> findEstoqueBaixo(Sort sort);

    boolean existsByCodigo(String codigo);

    boolean existsByIdAndAtivoTrue(String id);

    long countByAtivoTrue();

    long countByAtivoTrueAndQuantidadeEstoqueLessThanEqual(int quantidade);

    @Query(value = "{ 'ativo': true, $expr: { $lte: ['$quantidadeEstoque', '$estoqueMinimo'] } }", count = true)
    long countEstoqueBaixo();
}

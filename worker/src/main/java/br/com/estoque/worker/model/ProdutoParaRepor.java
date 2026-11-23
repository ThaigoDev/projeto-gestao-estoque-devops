package br.com.estoque.worker.model;

/**
 * Produto encontrado com estoque zerado em uma verificacao.
 *
 * @param novaReposicao true quando esta verificacao criou o registro em "reposicoes";
 *                      false quando ja existia uma reposicao PENDENTE para o produto.
 */
public record ProdutoParaRepor(
        String produtoId,
        String produtoNome,
        int quantidadeAtual,
        boolean novaReposicao) {
}

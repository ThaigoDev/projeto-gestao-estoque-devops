package br.com.estoque.api.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.estoque.api.dto.FornecedorRequest;
import br.com.estoque.api.exception.RecursoNaoEncontradoException;
import br.com.estoque.api.model.Fornecedor;
import br.com.estoque.api.repository.FornecedorRepository;

@Service
public class FornecedorService {

    private static final Sort POR_NOME = Sort.by("nome");

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    public List<Fornecedor> listar(String nome) {
        if (nome != null && !nome.isBlank()) {
            return fornecedorRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nome.trim(), POR_NOME);
        }
        return fornecedorRepository.findByAtivoTrue(POR_NOME);
    }

    public Fornecedor buscar(String id) {
        return fornecedorRepository.findById(id)
                .filter(Fornecedor::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor nao encontrado: " + id));
    }

    public Fornecedor criar(FornecedorRequest req) {
        Fornecedor fornecedor = new Fornecedor();
        preencher(fornecedor, req);
        return salvar(fornecedor);
    }

    public Fornecedor atualizar(String id, FornecedorRequest req) {
        Fornecedor fornecedor = buscar(id);
        preencher(fornecedor, req);
        return salvar(fornecedor);
    }

    public void desativar(String id) {
        Fornecedor fornecedor = buscar(id);
        fornecedor.setAtivo(false);
        fornecedorRepository.save(fornecedor);
    }

    /**
     * Resolve o fornecedor de uma entrada de mercadoria.
     * Prioridade: fornecedorId; senao, procura pelo nome e cadastra se nao existir.
     * @return null quando nenhum dos dois foi informado
     */
    public Fornecedor resolver(String fornecedorId, String nome) {
        if (fornecedorId != null && !fornecedorId.isBlank()) {
            return buscar(fornecedorId.trim());
        }
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String chave = Fornecedor.normalizarNome(nome);
        return fornecedorRepository.findByNomeBusca(chave)
                .map(this::reativarSeNecessario)
                .orElseGet(() -> {
                    try {
                        return criar(new FornecedorRequest(nome, null, null, null));
                    } catch (DuplicateKeyException corrida) {
                        // outra requisicao criou o mesmo fornecedor ao mesmo tempo
                        return fornecedorRepository.findByNomeBusca(chave).orElseThrow();
                    }
                });
    }

    private Fornecedor reativarSeNecessario(Fornecedor fornecedor) {
        if (!fornecedor.isAtivo()) {
            fornecedor.setAtivo(true);
            return fornecedorRepository.save(fornecedor);
        }
        return fornecedor;
    }

    private Fornecedor salvar(Fornecedor fornecedor) {
        try {
            return fornecedorRepository.save(fornecedor);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Ja existe um fornecedor com este nome");
        }
    }

    private static void preencher(Fornecedor fornecedor, FornecedorRequest req) {
        String nome = req.nome().trim().replaceAll("\\s+", " ");
        fornecedor.setNome(nome);
        fornecedor.setNomeBusca(Fornecedor.normalizarNome(nome));
        fornecedor.setCnpj(vazioParaNull(req.cnpj()));
        fornecedor.setTelefone(vazioParaNull(req.telefone()));
        fornecedor.setEmail(vazioParaNull(req.email()));
    }

    private static String vazioParaNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

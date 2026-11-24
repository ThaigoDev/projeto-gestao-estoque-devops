package br.com.estoque.api.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Formato padrao de erro para o frontend:
 * { "status": 422, "erro": "...", "mensagem": "...", "timestamp": "...", "campos": { ... } }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> estoqueInsuficiente(EstoqueInsuficienteException ex) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("produtoId", ex.getProdutoId());
        detalhes.put("disponivel", ex.getDisponivel());
        detalhes.put("solicitado", ex.getSolicitado());
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), detalhes);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraNegocio(RegraNegocioException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex) {
        Map<String, Object> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return erro(HttpStatus.BAD_REQUEST, "Dados invalidos", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> requisicaoMalFormada(Exception ex) {
        return erro(HttpStatus.BAD_REQUEST, "Requisicao mal formada: verifique o JSON e os parametros", null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicado(DuplicateKeyException ex) {
        // Nossas mensagens comecam com "Ja existe"; as do driver do Mongo sao tecnicas demais para a tela
        String mensagem = ex.getMessage() != null && ex.getMessage().startsWith("Ja existe")
                ? ex.getMessage()
                : "Ja existe um registro com estes dados (codigo ou nome duplicado)";
        return erro(HttpStatus.CONFLICT, mensagem, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rotaInexistente(NoResourceFoundException ex) {
        return erro(HttpStatus.NOT_FOUND, "Rota nao encontrada", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", null);
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem, Map<String, Object> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status.value());
        corpo.put("erro", status.getReasonPhrase());
        corpo.put("mensagem", mensagem);
        corpo.put("timestamp", Instant.now().toString());
        if (campos != null) {
            corpo.put("detalhes", campos);
        }
        return ResponseEntity.status(status).body(corpo);
    }
}

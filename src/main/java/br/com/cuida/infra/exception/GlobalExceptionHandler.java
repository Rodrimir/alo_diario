package br.com.cuida.infra.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte exceções no formato {@link ErroResponseDTO}. Os módulos sinalizam erros com as exceções
 * deste pacote ({@link RecursoNaoEncontradoException}, {@link RegraDeNegocioException}, ...), com a
 * mensagem em português.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BindException.class)
  ResponseEntity<ErroResponseDTO> validation(BindException ex) {
    List<ErroResponseDTO.FieldError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ErroResponseDTO.FieldError(
                        fe.getField(), String.valueOf(fe.getDefaultMessage())))
            .toList();
    return invalid(errors);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ErroResponseDTO> methodValidation(HandlerMethodValidationException ex) {
    List<ErroResponseDTO.FieldError> errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new ErroResponseDTO.FieldError(
                                    result.getMethodParameter().getParameterName(),
                                    String.valueOf(error.getDefaultMessage()))))
            .toList();
    return invalid(errors);
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  ResponseEntity<ErroResponseDTO> naoEncontrado(RecursoNaoEncontradoException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(RegraDeNegocioException.class)
  ResponseEntity<ErroResponseDTO> regraDeNegocio(RegraDeNegocioException ex) {
    return error(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
  }

  @ExceptionHandler(ConflitoException.class)
  ResponseEntity<ErroResponseDTO> conflito(ConflitoException ex) {
    return error(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(AcessoNegadoException.class)
  ResponseEntity<ErroResponseDTO> acessoNegado(AcessoNegadoException ex) {
    return error(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(NaoAutenticadoException.class)
  ResponseEntity<ErroResponseDTO> naoAutenticado(NaoAutenticadoException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(RecursoRemovidoException.class)
  ResponseEntity<ErroResponseDTO> removido(RecursoRemovidoException ex) {
    return error(HttpStatus.GONE, ex.getMessage());
  }

  @ExceptionHandler(ValidacaoException.class)
  ResponseEntity<ErroResponseDTO> invalidRequest(ValidacaoException ex) {
    return invalid(ex.errors());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErroResponseDTO> unreadable(HttpMessageNotReadableException ex) {
    return error(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErroResponseDTO> typeMismatch(MethodArgumentTypeMismatchException ex) {
    return error(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ErroResponseDTO> missingParameter(MissingServletRequestParameterException ex) {
    return error(
        HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErroResponseDTO> noResource(NoResourceFoundException ex) {
    return error(HttpStatus.NOT_FOUND, "Endpoint não encontrado: /" + ex.getResourcePath());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErroResponseDTO> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado: " + ex.getMethod());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErroResponseDTO> mediaType(HttpMediaTypeNotSupportedException ex) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado");
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ErroResponseDTO> responseStatus(ResponseStatusException ex) {
    String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
    return error(ex.getStatusCode(), reason);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErroResponseDTO> accessDenied(AccessDeniedException ex) {
    return error(HttpStatus.FORBIDDEN, "Acesso negado");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ErroResponseDTO> concurrentUpdate(OptimisticLockingFailureException ex) {
    return error(HttpStatus.CONFLICT, "O recurso foi alterado por outra operação; tente de novo");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErroResponseDTO> integrity(DataIntegrityViolationException ex) {
    log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
    return error(HttpStatus.CONFLICT, "Conflito com dados já existentes");
  }

  /** Nunca devolve a mensagem da exceção: pode conter SQL, caminhos ou dados pessoais. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ErroResponseDTO> generic(Exception ex) {
    log.error("Erro interno não tratado", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
  }

  private static ResponseEntity<ErroResponseDTO> invalid(List<ErroResponseDTO.FieldError> errors) {
    return ResponseEntity.badRequest()
        .body(new ErroResponseDTO(HttpStatus.BAD_REQUEST.value(), "Dados inválidos", errors));
  }

  private static ResponseEntity<ErroResponseDTO> error(HttpStatusCode status, String message) {
    return ResponseEntity.status(status).body(ErroResponseDTO.of(status, message));
  }
}

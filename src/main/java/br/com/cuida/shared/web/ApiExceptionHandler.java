package br.com.cuida.shared.web;

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
 * Converte exceções no formato {@link ApiError}. Os módulos sinalizam erros de negócio com {@link
 * ResponseStatusException} (status + mensagem em português).
 */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(BindException.class)
  ResponseEntity<ApiError> validation(BindException ex) {
    List<ApiError.FieldError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ApiError.FieldError(fe.getField(), String.valueOf(fe.getDefaultMessage())))
            .toList();
    return invalid(errors);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiError> methodValidation(HandlerMethodValidationException ex) {
    List<ApiError.FieldError> errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new ApiError.FieldError(
                                    result.getMethodParameter().getParameterName(),
                                    String.valueOf(error.getDefaultMessage()))))
            .toList();
    return invalid(errors);
  }

  @ExceptionHandler(InvalidRequestException.class)
  ResponseEntity<ApiError> invalidRequest(InvalidRequestException ex) {
    return invalid(ex.errors());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
    return error(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
    return error(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException ex) {
    return error(
        HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> noResource(NoResourceFoundException ex) {
    return error(HttpStatus.NOT_FOUND, "Endpoint não encontrado: /" + ex.getResourcePath());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado: " + ex.getMethod());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException ex) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado");
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> responseStatus(ResponseStatusException ex) {
    String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
    return error(ex.getStatusCode(), reason);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) {
    return error(HttpStatus.FORBIDDEN, "Acesso negado");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ApiError> concurrentUpdate(OptimisticLockingFailureException ex) {
    return error(HttpStatus.CONFLICT, "O recurso foi alterado por outra operação; tente de novo");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
    log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
    return error(HttpStatus.CONFLICT, "Conflito com dados já existentes");
  }

  /** Nunca devolve a mensagem da exceção: pode conter SQL, caminhos ou dados pessoais. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> generic(Exception ex) {
    log.error("Erro interno não tratado", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
  }

  private static ResponseEntity<ApiError> invalid(List<ApiError.FieldError> errors) {
    return ResponseEntity.badRequest()
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Dados inválidos", errors));
  }

  private static ResponseEntity<ApiError> error(HttpStatusCode status, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status, message));
  }
}

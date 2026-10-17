package br.com.cuida.shared.web;

import java.util.List;

/**
 * Erro de validação montado pela aplicação (ex.: listas no corpo), devolvido no mesmo formato da
 * validação automática: {@code {"status": 400, "message": "...", "errors": [...]}}.
 */
public class InvalidRequestException extends RuntimeException {

  private final transient List<ApiError.FieldError> errors;

  public InvalidRequestException(List<ApiError.FieldError> errors) {
    super("Dados inválidos");
    this.errors = List.copyOf(errors);
  }

  public static InvalidRequestException of(String field, String message) {
    return new InvalidRequestException(List.of(new ApiError.FieldError(field, message)));
  }

  public List<ApiError.FieldError> errors() {
    return errors;
  }
}

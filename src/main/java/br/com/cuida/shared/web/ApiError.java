package br.com.cuida.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.http.HttpStatusCode;

/**
 * Formato único de erro da API.
 *
 * <pre>
 * {"status": 404, "message": "Idoso não encontrado: 3f2c..."}
 * {"status": 400, "message": "Dados inválidos", "errors": [{"field": "name", "message": "..."}]}
 * </pre>
 *
 * {@code errors} só aparece em erro de validação.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, List<FieldError> errors) {

  public static ApiError of(HttpStatusCode status, String message) {
    return new ApiError(status.value(), message, null);
  }

  /** Erro de um campo da requisição. */
  public record FieldError(String field, String message) {}
}

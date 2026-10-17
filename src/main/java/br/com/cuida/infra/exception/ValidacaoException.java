package br.com.cuida.infra.exception;

import java.util.List;

/**
 * Erro de validação montado pela aplicação (ex.: listas no corpo), devolvido no mesmo formato da
 * validação automática: {@code {"status": 400, "message": "...", "errors": [...]}}.
 */
public class ValidacaoException extends RuntimeException {

  private final transient List<ErroResponseDTO.FieldError> errors;

  public ValidacaoException(List<ErroResponseDTO.FieldError> errors) {
    super("Dados inválidos");
    this.errors = List.copyOf(errors);
  }

  public static ValidacaoException of(String field, String message) {
    return new ValidacaoException(List.of(new ErroResponseDTO.FieldError(field, message)));
  }

  public List<ErroResponseDTO.FieldError> errors() {
    return errors;
  }
}

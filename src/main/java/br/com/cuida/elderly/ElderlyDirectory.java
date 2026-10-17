package br.com.cuida.elderly;

import br.com.cuida.shared.security.CurrentUser;
import br.com.cuida.shared.security.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * API pública do módulo para os demais: consulta de idosos e checagem de acesso.
 *
 * <p>Idoso de outro tutor responde 404 (não 403), para não revelar que ele existe.
 */
@Service
@Transactional(readOnly = true)
public class ElderlyDirectory {

  private final ElderlyRepository repository;
  private final CurrentUserProvider currentUser;

  ElderlyDirectory(ElderlyRepository repository, CurrentUserProvider currentUser) {
    this.repository = repository;
    this.currentUser = currentUser;
  }

  /** Busca sem checar acesso (uso interno: agendador, pipeline). */
  public Optional<ElderlyView> find(UUID elderlyId) {
    return repository.findById(elderlyId).map(Elderly::toView);
  }

  /** O idoso, se o usuário autenticado puder acessá-lo; senão 404. */
  public ElderlyView requireAccessible(UUID elderlyId) {
    CurrentUser user = currentUser.require();
    return repository
        .findById(elderlyId)
        .filter(elderly -> user.canAccess(elderly.getTutorId()))
        .map(Elderly::toView)
        .orElseThrow(() -> notFound(elderlyId));
  }

  static ResponseStatusException notFound(UUID elderlyId) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Idoso não encontrado: " + elderlyId);
  }
}

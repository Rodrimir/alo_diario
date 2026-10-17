package br.com.cuida.idoso;

import br.com.cuida.idoso.model.Idoso;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.infra.exception.RecursoNaoEncontradoException;
import br.com.cuida.infra.security.UsuarioLogado;
import br.com.cuida.infra.security.UsuarioLogadoService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API pública do módulo para os demais: consulta de idosos e checagem de acesso.
 *
 * <p>Idoso de outro tutor responde 404 (não 403), para não revelar que ele existe.
 */
@Service
@Transactional(readOnly = true)
public class AcessoIdosoService {

  private final IdosoRepository idosoRepository;
  private final UsuarioLogadoService usuarioLogadoService;

  AcessoIdosoService(IdosoRepository idosoRepository, UsuarioLogadoService usuarioLogadoService) {
    this.idosoRepository = idosoRepository;
    this.usuarioLogadoService = usuarioLogadoService;
  }

  /** Busca sem checar acesso (uso interno: agendador, pipeline). */
  public Optional<IdosoDTO> find(UUID elderlyId) {
    return idosoRepository.findById(elderlyId).map(Idoso::toDTO);
  }

  /** O idoso, se o usuário autenticado puder acessá-lo; senão 404. */
  public IdosoDTO requireAccessible(UUID elderlyId) {
    UsuarioLogado user = usuarioLogadoService.require();
    return idosoRepository
        .findById(elderlyId)
        .filter(elderly -> user.canAccess(elderly.getTutorId()))
        .map(Idoso::toDTO)
        .orElseThrow(() -> notFound(elderlyId));
  }

  static RecursoNaoEncontradoException notFound(UUID elderlyId) {
    return new RecursoNaoEncontradoException("Idoso não encontrado: " + elderlyId);
  }
}

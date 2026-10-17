package br.com.cuida.infra.security;

import br.com.cuida.infra.exception.NaoAutenticadoException;
import br.com.cuida.infra.jwt.ClaimsJwt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Lê o usuário autenticado do contexto do Spring Security. */
@Component
public class UsuarioLogadoService {

  public Optional<UsuarioLogado> find() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return Optional.empty();
    }
    String role = jwt.getClaimAsString(ClaimsJwt.ROLE);
    if (role == null) {
      return Optional.empty();
    }
    return Optional.of(
        new UsuarioLogado(UUID.fromString(jwt.getSubject()), PapelUsuario.valueOf(role)));
  }

  public UsuarioLogado require() {
    return find().orElseThrow(() -> new NaoAutenticadoException("Não autenticado"));
  }
}

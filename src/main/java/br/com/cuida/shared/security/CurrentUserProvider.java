package br.com.cuida.shared.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Lê o usuário autenticado do contexto do Spring Security. */
@Component
public class CurrentUserProvider {

  public Optional<CurrentUser> find() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return Optional.empty();
    }
    String role = jwt.getClaimAsString(JwtClaims.ROLE);
    if (role == null) {
      return Optional.empty();
    }
    return Optional.of(new CurrentUser(UUID.fromString(jwt.getSubject()), UserRole.valueOf(role)));
  }

  public CurrentUser require() {
    return find()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado"));
  }
}

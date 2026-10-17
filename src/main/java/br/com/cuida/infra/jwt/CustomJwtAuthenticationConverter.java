package br.com.cuida.infra.jwt;

import br.com.cuida.infra.security.PapelUsuario;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Converte o claim {@code role} do token na authority {@code ROLE_<PAPEL>} do Spring Security. */
@Component
public class CustomJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String role = jwt.getClaimAsString(ClaimsJwt.ROLE);
    List<SimpleGrantedAuthority> authorities =
        role == null
            ? List.of()
            : List.of(new SimpleGrantedAuthority(PapelUsuario.valueOf(role).authority()));
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }
}

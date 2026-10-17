package br.com.cuida.infra.jwt;

import br.com.cuida.infra.security.PapelUsuario;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite JWTs HS256 com {@code sub} = id do usuário e {@code role} = papel. */
@Component
public class TokenService {

  private final JwtEncoder encoder;
  private final JwtProperties jwtProperties;
  private final Clock clock;

  TokenService(JwtEncoder encoder, JwtProperties jwtProperties, Clock clock) {
    this.encoder = encoder;
    this.jwtProperties = jwtProperties;
    this.clock = clock;
  }

  public TokenEmitido issue(UUID userId, PapelUsuario role) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(jwtProperties.ttl()))
            .claim(ClaimsJwt.ROLE, role.name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenEmitido(token, jwtProperties.ttl().toSeconds());
  }
}

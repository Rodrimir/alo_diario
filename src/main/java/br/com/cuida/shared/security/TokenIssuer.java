package br.com.cuida.shared.security;

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
public class TokenIssuer {

  private final JwtEncoder encoder;
  private final JwtProperties properties;
  private final Clock clock;

  TokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public IssuedToken issue(UUID userId, UserRole role) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.ttl()))
            .claim(JwtClaims.ROLE, role.name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(token, properties.ttl().toSeconds());
  }
}

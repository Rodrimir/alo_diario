package br.com.cuida.infra.jwt;

import br.com.cuida.infra.security.PapelUsuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

  /**
   * Token curto para a IA de voz chamar o servidor MCP durante uma ligação. Só vale em {@code /mcp}
   * (audiência {@value ClaimsJwt#AUDIENCE_MCP}) e só para esta ligação.
   */
  public String emitirTokenDeLigacao(UUID callId, Duration validade) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer())
            .subject("call:" + callId)
            .audience(List.of(ClaimsJwt.AUDIENCE_MCP))
            .issuedAt(now)
            .expiresAt(now.plus(validade))
            .claim(ClaimsJwt.CALL_ID, callId.toString())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}

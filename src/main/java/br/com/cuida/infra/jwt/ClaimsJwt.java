package br.com.cuida.infra.jwt;

/**
 * Nomes dos claims próprios do token. No token de usuário, {@code sub} é o id do usuário; no token
 * de ligação (MCP), {@code sub} é {@code call:<id>} e {@code aud} é {@value #AUDIENCE_MCP}.
 */
public final class ClaimsJwt {

  public static final String ROLE = "role";

  /** Id da ligação no token usado pela IA de voz para chamar o servidor MCP. */
  public static final String CALL_ID = "call_id";

  /** Audiência dos tokens de ligação: só valem no {@code /mcp}. */
  public static final String AUDIENCE_MCP = "mcp";

  private ClaimsJwt() {}
}

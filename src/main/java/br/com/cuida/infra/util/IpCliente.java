package br.com.cuida.infra.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP de quem fez a requisição. Atrás de proxy, o perfil {@code prod} usa {@code
 * server.forward-headers-strategy=framework}, então {@code getRemoteAddr()} já reflete o cliente.
 */
public final class IpCliente {

  private IpCliente() {}

  public static String of(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
  }
}

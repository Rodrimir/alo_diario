package br.com.cuida.ligacao.mock;

/**
 * Cenário simulado, escolhido pelos 4 últimos dígitos do telefone do idoso:
 *
 * <ul>
 *   <li>{@code 0000}: não atende
 *   <li>{@code 1111}: ocupado
 *   <li>{@code 2222}: erro do gateway ao discar
 *   <li>{@code 9999}: conversa com relato de queda (alerta urgente)
 *   <li>qualquer outro: conversa normal
 * </ul>
 */
enum CenarioSimulado {
  NORMAL,
  NO_ANSWER,
  BUSY,
  DIAL_ERROR,
  URGENT;

  static CenarioSimulado forPhone(String phoneNumber) {
    if (phoneNumber == null) {
      return NORMAL;
    }
    if (phoneNumber.endsWith("0000")) {
      return NO_ANSWER;
    }
    if (phoneNumber.endsWith("1111")) {
      return BUSY;
    }
    if (phoneNumber.endsWith("2222")) {
      return DIAL_ERROR;
    }
    if (phoneNumber.endsWith("9999")) {
      return URGENT;
    }
    return NORMAL;
  }
}

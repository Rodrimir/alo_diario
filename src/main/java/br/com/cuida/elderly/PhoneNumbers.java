package br.com.cuida.elderly;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normaliza telefones para E.164. Aceita qualquer formatação ({@code (11) 98765-4321}, {@code
 * 5511987654321}, {@code +55 11 98765-4321}); números brasileiros de 10 ou 11 dígitos (DDD +
 * número) ganham o DDI 55.
 */
final class PhoneNumbers {

  private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

  private PhoneNumbers() {}

  static Optional<String> toE164(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    boolean international = raw.trim().startsWith("+");
    String digits = raw.replaceAll("\\D", "");
    if (!international && (digits.length() == 10 || digits.length() == 11)) {
      digits = "55" + digits;
    }
    String candidate = "+" + digits;
    return E164.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
  }
}

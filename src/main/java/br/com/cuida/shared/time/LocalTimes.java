package br.com.cuida.shared.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Conversões para a saída da API. Datas são gravadas em UTC ({@code timestamptz}) e devolvidas como
 * {@link LocalDateTime} (sem offset, ex.: {@code "2026-10-17T09:00:00"}) no fuso da pessoa idosa,
 * que vem junto no cadastro ({@code timezone}), com precisão de segundos.
 */
public final class LocalTimes {

  private LocalTimes() {}

  public static LocalDateTime at(Instant instant, ZoneId zone) {
    return instant == null
        ? null
        : LocalDateTime.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), zone);
  }
}

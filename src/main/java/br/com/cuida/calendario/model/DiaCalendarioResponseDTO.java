package br.com.cuida.calendario.model;

import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.StatusLigacao;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia do calendário. Mostra a ligação mais relevante do dia (a última processada, senão a última
 * tentativa); {@code attempts} conta todas, inclusive as manuais.
 */
public record DiaCalendarioResponseDTO(
    LocalDate date,
    UUID callId,
    StatusLigacao status,
    NivelAlerta alertLevel,
    Humor mood,
    String summaryText,
    int attempts,
    boolean hasTranscript,
    boolean hasAudio) {}

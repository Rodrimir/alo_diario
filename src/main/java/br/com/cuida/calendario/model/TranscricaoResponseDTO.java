package br.com.cuida.calendario.model;

import br.com.cuida.ligacao.model.OrigemTranscricao;
import java.util.UUID;

/**
 * Transcrição completa de uma ligação.
 *
 * @param source {@code PROVIDER} (gateway de voz) ou {@code WHISPER} (nossa transcrição)
 */
public record TranscricaoResponseDTO(UUID callId, OrigemTranscricao source, String transcript) {}

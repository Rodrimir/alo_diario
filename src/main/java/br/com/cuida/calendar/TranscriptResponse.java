package br.com.cuida.calendar;

import br.com.cuida.call.TranscriptSource;
import java.util.UUID;

/**
 * Transcrição completa de uma ligação.
 *
 * @param source {@code PROVIDER} (gateway de voz) ou {@code WHISPER} (nossa transcrição)
 */
record TranscriptResponse(UUID callId, TranscriptSource source, String transcript) {}

package br.com.cuida.recording;

import br.com.cuida.elderly.ElderlyDeleted;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/** API pública do módulo: copia, lê e apaga gravações. */
@Service
public class Recordings {

  private static final Logger log = LoggerFactory.getLogger(Recordings.class);

  private final AudioStorage storage;
  private final RecordingFetcher fetcher;

  Recordings(AudioStorage storage, RecordingFetcher fetcher) {
    this.storage = storage;
    this.fetcher = fetcher;
  }

  /** Baixa a gravação do provedor e guarda em {@code elderly/{elderlyId}/calls/{callId}.ext}. */
  public StoredRecording storeFromProvider(UUID callId, UUID elderlyId, String sourceUrl) {
    RecordingFetcher.Download download = fetcher.fetch(sourceUrl);
    String key =
        prefixOf(elderlyId)
            + "calls/"
            + callId
            + "."
            + AudioFormats.extensionOf(download.contentType());
    storage.put(key, download.content(), download.contentType());
    return new StoredRecording(key, download.contentType(), download.content().length);
  }

  /** Abre o áudio para leitura (quem chama fecha o stream). */
  public Optional<AudioContent> open(String objectKey) {
    return storage.get(objectKey);
  }

  public void delete(String objectKey) {
    storage.delete(objectKey);
  }

  /** Exclusão LGPD: apaga todos os áudios do idoso depois que o cadastro sai do banco. */
  @ApplicationModuleListener
  void on(ElderlyDeleted event) {
    int deleted = storage.deletePrefix(prefixOf(event.elderlyId()));
    log.info("{} gravação(ões) do idoso {} apagada(s)", deleted, event.elderlyId());
  }

  static String prefixOf(UUID elderlyId) {
    return "elderly/" + elderlyId + "/";
  }
}

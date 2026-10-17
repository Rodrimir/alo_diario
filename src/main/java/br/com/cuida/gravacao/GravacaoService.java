package br.com.cuida.gravacao;

import br.com.cuida.gravacao.model.ConteudoAudio;
import br.com.cuida.gravacao.model.GravacaoArmazenada;
import br.com.cuida.idoso.model.IdosoExcluido;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/** API pública do módulo: copia, lê e apaga gravações. */
@Service
public class GravacaoService {

  private static final Logger log = LoggerFactory.getLogger(GravacaoService.class);

  private final ArmazenamentoDeAudio armazenamentoDeAudio;
  private final DownloadDeGravacao downloadDeGravacao;

  GravacaoService(
      ArmazenamentoDeAudio armazenamentoDeAudio, DownloadDeGravacao downloadDeGravacao) {
    this.armazenamentoDeAudio = armazenamentoDeAudio;
    this.downloadDeGravacao = downloadDeGravacao;
  }

  /** Baixa a gravação do provedor e guarda em {@code elderly/{elderlyId}/calls/{callId}.ext}. */
  public GravacaoArmazenada storeFromProvider(UUID callId, UUID elderlyId, String sourceUrl) {
    DownloadDeGravacao.Download download = downloadDeGravacao.fetch(sourceUrl);
    String key =
        prefixOf(elderlyId)
            + "calls/"
            + callId
            + "."
            + FormatosDeAudio.extensionOf(download.contentType());
    armazenamentoDeAudio.put(key, download.content(), download.contentType());
    return new GravacaoArmazenada(key, download.contentType(), download.content().length);
  }

  /** Abre o áudio para leitura (quem chama fecha o stream). */
  public Optional<ConteudoAudio> open(String objectKey) {
    return armazenamentoDeAudio.get(objectKey);
  }

  public void delete(String objectKey) {
    armazenamentoDeAudio.delete(objectKey);
  }

  /** Exclusão LGPD: apaga todos os áudios do idoso depois que o cadastro sai do banco. */
  @ApplicationModuleListener
  void on(IdosoExcluido event) {
    int deleted = armazenamentoDeAudio.deletePrefix(prefixOf(event.elderlyId()));
    log.info("{} gravação(ões) do idoso {} apagada(s)", deleted, event.elderlyId());
  }

  static String prefixOf(UUID elderlyId) {
    return "elderly/" + elderlyId + "/";
  }
}

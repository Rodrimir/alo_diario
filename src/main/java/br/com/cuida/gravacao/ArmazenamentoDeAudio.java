package br.com.cuida.gravacao;

import br.com.cuida.gravacao.model.ConteudoAudio;
import java.util.Optional;

/** Porta do armazenamento de objetos de áudio. Adaptadores: S3 (MinIO/AWS) e disco local. */
interface ArmazenamentoDeAudio {

  void put(String key, byte[] content, String contentType);

  Optional<ConteudoAudio> get(String key);

  void delete(String key);

  /** Apaga tudo sob o prefixo (ex.: {@code elderly/{id}/}). @return quantos objetos */
  int deletePrefix(String prefix);
}

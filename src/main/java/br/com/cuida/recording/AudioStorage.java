package br.com.cuida.recording;

import java.util.Optional;

/** Porta do armazenamento de objetos de áudio. Adaptadores: S3 (MinIO/AWS) e disco local. */
interface AudioStorage {

  void put(String key, byte[] content, String contentType);

  Optional<AudioContent> get(String key);

  void delete(String key);

  /** Apaga tudo sob o prefixo (ex.: {@code elderly/{id}/}). @return quantos objetos */
  int deletePrefix(String prefix);
}

package br.com.cuida.recording;

import java.util.Locale;
import java.util.Map;

/** Extensão de arquivo ↔ tipo MIME dos formatos de áudio que aceitamos. */
final class AudioFormats {

  static final String DEFAULT_TYPE = "audio/mpeg";

  private static final Map<String, String> EXTENSION_BY_TYPE =
      Map.of(
          "audio/mpeg", "mp3",
          "audio/mp3", "mp3",
          "audio/wav", "wav",
          "audio/x-wav", "wav",
          "audio/wave", "wav",
          "audio/ogg", "ogg",
          "audio/webm", "webm",
          "audio/mp4", "m4a",
          "audio/x-m4a", "m4a",
          "audio/flac", "flac");

  private static final Map<String, String> TYPE_BY_EXTENSION =
      Map.of(
          "mp3", "audio/mpeg",
          "wav", "audio/wav",
          "ogg", "audio/ogg",
          "webm", "audio/webm",
          "m4a", "audio/mp4",
          "flac", "audio/flac");

  private AudioFormats() {}

  /** Tipo normalizado a partir do Content-Type recebido e/ou da extensão da URL. */
  static String normalize(String contentType, String url) {
    if (contentType != null) {
      String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
      if (EXTENSION_BY_TYPE.containsKey(base)) {
        return TYPE_BY_EXTENSION.get(EXTENSION_BY_TYPE.get(base));
      }
    }
    return contentTypeOf(url);
  }

  static String contentTypeOf(String pathOrUrl) {
    if (pathOrUrl == null) {
      return DEFAULT_TYPE;
    }
    String clean = pathOrUrl.split("\\?")[0];
    int dot = clean.lastIndexOf('.');
    if (dot < 0) {
      return DEFAULT_TYPE;
    }
    return TYPE_BY_EXTENSION.getOrDefault(
        clean.substring(dot + 1).toLowerCase(Locale.ROOT), DEFAULT_TYPE);
  }

  static String extensionOf(String contentType) {
    return EXTENSION_BY_TYPE.getOrDefault(contentType, "mp3");
  }
}

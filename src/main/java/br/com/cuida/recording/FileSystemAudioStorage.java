package br.com.cuida.recording;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Armazenamento em pasta local (dev sem MinIO e testes). O tipo MIME é deduzido da extensão da
 * chave, que sempre é gerada por nós.
 */
class FileSystemAudioStorage implements AudioStorage {

  private final Path root;

  FileSystemAudioStorage(Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public void put(String key, byte[] content, String contentType) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Path temp = Files.createTempFile(target.getParent(), ".upload", ".tmp");
      Files.write(temp, content);
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Optional<AudioContent> get(String key) {
    Path file = resolve(key);
    try {
      return Optional.of(
          new AudioContent(
              Files.newInputStream(file), Files.size(file), AudioFormats.contentTypeOf(key)));
    } catch (NoSuchFileException e) {
      return Optional.empty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public int deletePrefix(String prefix) {
    Path directory = resolve(prefix);
    if (!Files.isDirectory(directory)) {
      return 0;
    }
    try (Stream<Path> walk = Files.walk(directory)) {
      List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
      int files = 0;
      for (Path path : paths) {
        if (Files.isRegularFile(path)) {
          files++;
        }
        Files.deleteIfExists(path);
      }
      return files;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Impede que uma chave escape da pasta raiz ({@code ../}). */
  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("Chave fora do armazenamento: " + key);
    }
    return path;
  }
}

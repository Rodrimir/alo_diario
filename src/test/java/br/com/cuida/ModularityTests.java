package br.com.cuida;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Quebra o build se houver ciclo entre módulos, acesso a classe interna de outro módulo ou
 * dependência fora do {@code allowedDependencies} declarado em cada {@code package-info.java}.
 */
class ModularityTests {

  private static final Path DOCS = Path.of("docs/modules");

  private final ApplicationModules modules = ApplicationModules.of(CuidaApplication.class);

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }

  @Test
  void writesModuleDocumentation() throws IOException {
    new Documenter(modules, Documenter.Options.defaults().withOutputFolder(DOCS.toString()))
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml()
        .writeModuleCanvases();
    sortRelationsInDiagrams();
  }

  /**
   * O Documenter emite as relações {@code Rel(...)} em ordem não determinística; ordená-las evita
   * diffs espúrios em {@code docs/modules} a cada build.
   */
  private static void sortRelationsInDiagrams() throws IOException {
    try (Stream<Path> files = Files.list(DOCS)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".puml")).toList()) {
        List<String> lines = Files.readAllLines(file);
        List<Integer> positions = new ArrayList<>();
        List<String> relations = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
          if (lines.get(i).startsWith("Rel(")) {
            positions.add(i);
            relations.add(lines.get(i));
          }
        }
        relations.sort(null);
        List<String> sorted = new ArrayList<>(lines);
        for (int i = 0; i < positions.size(); i++) {
          sorted.set(positions.get(i), relations.get(i));
        }
        if (!sorted.equals(lines)) {
          Files.write(file, sorted);
        }
      }
    }
  }
}

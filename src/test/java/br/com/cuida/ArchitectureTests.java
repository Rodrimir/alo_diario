package br.com.cuida;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Regras de organização que o Spring Modulith não cobre sozinho. */
class ArchitectureTests {

  private static final String BASE = "br.com.cuida";

  private static final String[] FEATURES = {
    "autenticacao", "idoso", "agenda", "questionario", "ligacao",
    "webhook", "analise", "gravacao", "calendario", "auditoria"
  };

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  @Test
  void noPackagesByTechnicalLayer() {
    noClasses()
        .should()
        .resideInAnyPackage(
            "..controllers..",
            "..services..",
            "..repositories..",
            "..models..",
            "..dtos..",
            "..managers..",
            "..utils..")
        .because("o código é organizado por funcionalidade (package by feature)")
        .check(classes);
  }

  @Test
  void infraDoesNotDependOnFeatures() {
    String[] featurePackages = new String[FEATURES.length];
    for (int i = 0; i < FEATURES.length; i++) {
      featurePackages[i] = BASE + "." + FEATURES[i] + "..";
    }
    noClasses()
        .that()
        .resideInAPackage(BASE + ".infra..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(featurePackages)
        .because("infra contém apenas infraestrutura transversal")
        .check(classes);
  }
}

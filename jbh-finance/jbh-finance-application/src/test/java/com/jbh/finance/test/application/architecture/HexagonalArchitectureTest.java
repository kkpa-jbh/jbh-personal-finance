package com.jbh.finance.test.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HexagonalArchitectureTest {

  private static final String DOMAIN_PACKAGE = "com.jbh.finance.domain..";
  private static final String APPLICATION_PACKAGE = "com.jbh.finance.application..";
  private static final String INFRA_PACKAGE = "com.jbh.finance.infra..";
  private static final String[] FRAMEWORK_PACKAGES = {
    "jakarta..", "io.quarkus..", "org.hibernate..", "org.eclipse.microprofile.."
  };

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.jbh.finance.domain", "com.jbh.finance.application");
  }

  @Test
  void domainShouldNotDependOnApplicationOrInfra() {
    noClasses()
        .that()
        .resideInAPackage(DOMAIN_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(APPLICATION_PACKAGE, INFRA_PACKAGE)
        .check(classes);
  }

  @Test
  void applicationShouldNotDependOnInfra() {
    noClasses()
        .that()
        .resideInAPackage(APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(INFRA_PACKAGE)
        .check(classes);
  }

  @Test
  void domainAndApplicationShouldNotUseFrameworkClasses() {
    noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(FRAMEWORK_PACKAGES)
        .check(classes);
  }

  @Test
  void useCasePackagesShouldContainOnlyInterfaces() {
    classes()
        .that()
        .resideInAPackage("..application.feature..usecases")
        .should()
        .beInterfaces()
        .check(classes);
  }
}

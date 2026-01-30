package com.jbh.commons.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.jbh.commons")
class CommonsArchitectureTest {

  @ArchTest
  static final ArchRule commons_should_not_depend_on_domain =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..domain..", "..application..");

  @ArchTest
  static final ArchRule commons_should_not_use_spring =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..", "jakarta.persistence..", "javax.persistence..");

  @ArchTest
  static final ArchRule commons_should_only_use_allowed_packages =
      noClasses()
          .should()
          .resideOutsideOfPackages(
              "com.jbh.commons.api..",
              "com.jbh.commons.exception..",
              "com.jbh.commons.util..",
              "com.jbh.commons.time..",
              "com.jbh.commons.ids..",
              "com.jbh.commons.validation..",
              "com.jbh.commons.logging..",
              "com.jbh.commons.architecture..");

  @ArchTest
  static final ArchRule commons_should_not_use_quarkus_cdi =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("jakarta.enterprise..", "io.quarkus.arc..");
}

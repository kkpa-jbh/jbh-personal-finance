package com.jbh.finance.test.application.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class CategoryArchitectureTest {

  private static final String APPLICATION_PACKAGE = "com.jbh.finance.application..";
  private static final String CATEGORY_SERVICE_IMPL =
      "com.jbh.finance.application.feature.category.services.CategoryServiceImpl";
  private static final String CATEGORY_DTO =
      "com.jbh.finance.application.feature.category.dto.CategoryDTO";
  private static final String SYSTEM_CATEGORY_ALIAS =
      "com.jbh.finance.domain.category.vo.SystemCategoryAlias";

  @Test
  void onlyCategoryServiceImplAndCategoryDTOShouldUseSystemCategoryAlias() {
    final JavaClasses classes =
        new ClassFileImporter().importPackages("com.jbh.finance.application");

    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(APPLICATION_PACKAGE)
            .and()
            .doNotHaveFullyQualifiedName(CATEGORY_SERVICE_IMPL)
            .and()
            .doNotHaveFullyQualifiedName(CATEGORY_DTO)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(SYSTEM_CATEGORY_ALIAS);

    rule.check(classes);
  }
}

package com.jbh.finance.infra.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.infra.adapters.out.persistence.monthlybalance.MonthlyBalanceJPAEntity;
import com.jbh.finance.infra.adapters.out.persistence.movement.MovementJPAEntity;
import com.jbh.finance.infra.adapters.out.persistence.product.ProductJPAEntity;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit-style test to ensure DTOs and JPA Entities have matching fields. This test will fail at
 * compile/test time if fields don't match between DTO and Entity.
 */
public class DtoEntityFieldMatchingTest {

  @Test
  public void accountDTO_and_AccountJPAEntity_should_have_matching_fields() {
    // Get field names from both classes
    final Set<String> dtoFields = getFieldNames(ProductDTO.class);
    final Set<String> entityFields = getFieldNames(ProductJPAEntity.class);
    final String dtoClass = ProductDTO.class.getName();
    final String entityClass = ProductDTO.class.getName();

    // Assert they match
    assertMapping(dtoFields, entityFields, dtoClass, entityClass);
  }

  private Set<String> getFieldNames(final Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .map(Field::getName)
        .filter(name -> !name.startsWith("$")) // Exclude synthetic fields
        .collect(Collectors.toSet());
  }

  private void assertMapping(
      final Set<String> dtoFields,
      final Set<String> entityFields,
      final String dtoClass,
      final String entityClass) {
    assertEquals(
        dtoFields,
        entityFields,
        () -> {
          final Set<String> inDtoNotInEntity = difference(dtoFields, entityFields);
          final Set<String> inEntityNotInDto = difference(entityFields, dtoFields);

          final StringBuilder message = new StringBuilder("Field mismatch detected!\n");
          if (!inDtoNotInEntity.isEmpty()) {
            message
                .append(String.format("  Fields in %s but NOT in %s: ", dtoClass, entityClass))
                .append(inDtoNotInEntity)
                .append("\n");
          }

          if (!inEntityNotInDto.isEmpty()) {
            message
                .append(String.format("  Fields in %s but NOT in %s: ", entityClass, dtoClass))
                .append(inEntityNotInDto)
                .append("\n");
          }
          return message.toString();
        });
  }

  private Set<String> difference(final Set<String> set1, final Set<String> set2) {
    return set1.stream().filter(item -> !set2.contains(item)).collect(Collectors.toSet());
  }

  @Test
  public void monthlyBalanceDTO_and_MonthlyBalanceJPAEntity_should_have_matching_fields() {
    // Get field names from both classes
    final Set<String> dtoFields = getFieldNames(MonthlyBalanceDTO.class);
    final Set<String> entityFields = getFieldNames(MonthlyBalanceJPAEntity.class);
    final String dtoClass = MonthlyBalanceDTO.class.getName();
    final String entityClass = MonthlyBalanceJPAEntity.class.getName();

    // Assert they match
    assertMapping(dtoFields, entityFields, dtoClass, entityClass);
  }

  @Test
  public void movementDTO_and_MovementJPAEntity_should_have_matching_fields() {
    // Get field names from both classes
    final Set<String> dtoFields = getFieldNames(MovementDTO.class);
    final Set<String> entityFields = getFieldNames(MovementJPAEntity.class);
    final String dtoClass = MovementDTO.class.getName();
    final String entityClass = MovementJPAEntity.class.getName();

    // Assert they match
    assertMapping(dtoFields, entityFields, dtoClass, entityClass);
  }
}

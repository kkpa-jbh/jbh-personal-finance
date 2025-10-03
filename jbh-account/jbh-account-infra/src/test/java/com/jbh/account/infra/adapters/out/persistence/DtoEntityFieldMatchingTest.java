package com.jbh.account.infra.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.infra.adapters.out.persistence.account.AccountJPAEntity;
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
    final Set<String> dtoFields = getFieldNames(AccountDTO.class);
    final Set<String> entityFields = getFieldNames(AccountJPAEntity.class);

    // Assert they match
    assertEquals(
        dtoFields,
        entityFields,
        () -> {
          final Set<String> inDtoNotInEntity = difference(dtoFields, entityFields);
          final Set<String> inEntityNotInDto = difference(entityFields, dtoFields);

          final StringBuilder message = new StringBuilder("Field mismatch detected!\n");
          if (!inDtoNotInEntity.isEmpty()) {
            message
                .append("  Fields in AccountDTO but NOT in AccountJPAEntity: ")
                .append(inDtoNotInEntity)
                .append("\n");
          }
          if (!inEntityNotInDto.isEmpty()) {
            message
                .append("  Fields in AccountJPAEntity but NOT in AccountDTO: ")
                .append(inEntityNotInDto)
                .append("\n");
          }
          return message.toString();
        });
  }

  private Set<String> getFieldNames(final Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .map(Field::getName)
        .filter(name -> !name.startsWith("$")) // Exclude synthetic fields
        .collect(Collectors.toSet());
  }

  private Set<String> difference(final Set<String> set1, final Set<String> set2) {
    return set1.stream().filter(item -> !set2.contains(item)).collect(Collectors.toSet());
  }
}

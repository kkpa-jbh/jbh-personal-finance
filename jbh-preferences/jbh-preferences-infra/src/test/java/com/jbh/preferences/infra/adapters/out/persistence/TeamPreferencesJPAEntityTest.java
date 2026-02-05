package com.jbh.preferences.infra.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamPreferencesJPAEntityTest {

  @Test
  void toEntity_shouldConvertDTOToEntity() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("theme", "dark");

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("5000.00"))
            .metadata(PreferencesMetadata.fromMap(metadataMap))
            .lastModifiedBy(modifierId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    final TeamPreferencesJPAEntity entity = TeamPreferencesJPAEntity.toEntity(dto);

    assertEquals(teamId, entity.getTeamId());
    assertEquals(Currency.USD, entity.getDefaultCurrency());
    assertEquals(new BigDecimal("5000.00"), entity.getSavingsGoal());
    assertEquals(modifierId, entity.getLastModifiedBy());
    assertNotNull(entity.getMetadata());
    assertEquals("dark", entity.getMetadata().get("theme"));
    assertEquals(now, entity.getCreatedAt());
    assertEquals(now, entity.getUpdatedAt());
  }

  @Test
  void toEntity_shouldHandleNullLastModifiedBy() {
    final UUID teamId = UUID.randomUUID();
    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.defaultBuilder(teamId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    final TeamPreferencesJPAEntity entity = TeamPreferencesJPAEntity.toEntity(dto);

    assertNull(entity.getLastModifiedBy());
  }

  @Test
  void toDTO_shouldConvertEntityToDTO() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("key", "value");

    final TeamPreferencesJPAEntity entity = new TeamPreferencesJPAEntity();
    entity.setTeamId(teamId);
    entity.setDefaultCurrency(Currency.COP);
    entity.setSavingsGoal(new BigDecimal("1000.00"));
    entity.setMetadata(metadataMap);
    entity.setLastModifiedBy(modifierId);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    final TeamPreferencesDTO dto = entity.toDTO();

    assertEquals(teamId, dto.teamId());
    assertEquals(Currency.COP, dto.defaultCurrency());
    assertEquals(new BigDecimal("1000.00"), dto.savingsGoal());
    assertEquals(modifierId, dto.lastModifiedBy());
    assertNotNull(dto.metadata());
    assertEquals("value", dto.metadata().getData().get("key"));
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void toDTO_shouldHandleNullLastModifiedBy() {
    final TeamPreferencesJPAEntity entity = new TeamPreferencesJPAEntity();
    entity.setTeamId(UUID.randomUUID());
    entity.setDefaultCurrency(Currency.COP);
    entity.setSavingsGoal(BigDecimal.ZERO);
    entity.setLastModifiedBy(null);
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    final TeamPreferencesDTO dto = entity.toDTO();

    assertNull(dto.lastModifiedBy());
  }

  @Test
  void toDTO_shouldHandleNullMetadata() {
    final TeamPreferencesJPAEntity entity = new TeamPreferencesJPAEntity();
    entity.setTeamId(UUID.randomUUID());
    entity.setDefaultCurrency(Currency.COP);
    entity.setSavingsGoal(BigDecimal.ZERO);
    entity.setMetadata(null);
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    final TeamPreferencesDTO dto = entity.toDTO();

    assertNotNull(dto.metadata());
    assertTrue(dto.metadata().isEmpty());
  }

  @Test
  void roundTrip_shouldPreserveAllFields() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("setting1", "value1");
    metadataMap.put("setting2", 42);

    final TeamPreferencesDTO originalDTO =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("12345.67"))
            .metadata(PreferencesMetadata.fromMap(metadataMap))
            .lastModifiedBy(modifierId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    final TeamPreferencesJPAEntity entity = TeamPreferencesJPAEntity.toEntity(originalDTO);
    final TeamPreferencesDTO resultDTO = entity.toDTO();

    assertEquals(originalDTO.teamId(), resultDTO.teamId());
    assertEquals(originalDTO.defaultCurrency(), resultDTO.defaultCurrency());
    assertEquals(originalDTO.savingsGoal(), resultDTO.savingsGoal());
    assertEquals(originalDTO.lastModifiedBy(), resultDTO.lastModifiedBy());
    assertEquals(
        originalDTO.metadata().getData().get("setting1"),
        resultDTO.metadata().getData().get("setting1"));
    assertEquals(
        originalDTO.metadata().getData().get("setting2"),
        resultDTO.metadata().getData().get("setting2"));
  }
}

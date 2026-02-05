package com.jbh.preferences.application.core.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamPreferencesDTOTest {

  @Test
  void builder_shouldCreateDTOWithDefaultValues() {
    final UUID teamId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.builder()
            .teamId(teamId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(teamId, dto.teamId());
    assertEquals(Currency.defaultCurrency(), dto.defaultCurrency());
    assertEquals(JbhMoneyUtils.JBH_ZERO, dto.savingsGoal());
    assertTrue(dto.metadata().isEmpty());
    assertNull(dto.lastModifiedBy());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void defaultBuilder_shouldCreateDTOWithTeamIdAndDefaults() {
    final UUID teamId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.defaultBuilder(teamId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(teamId, dto.teamId());
    assertEquals(Currency.COP, dto.defaultCurrency());
    assertEquals(JbhMoneyUtils.JBH_ZERO, dto.savingsGoal());
    assertTrue(dto.metadata().isEmpty());
    assertNull(dto.lastModifiedBy());
  }

  @Test
  void internalBuilder_shouldAllowFullCustomization() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();
    final BigDecimal savingsGoal = new BigDecimal("1000.50");
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("key", "value");

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(savingsGoal)
            .metadata(metadata)
            .lastModifiedBy(modifierId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(teamId, dto.teamId());
    assertEquals(Currency.USD, dto.defaultCurrency());
    assertEquals(savingsGoal, dto.savingsGoal());
    assertEquals(metadata, dto.metadata());
    assertEquals(modifierId, dto.lastModifiedBy());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void builder_shouldAllowOverridingDefaults() {
    final UUID teamId = UUID.randomUUID();
    final BigDecimal customSavingsGoal = new BigDecimal("5000.00");
    final UUID modifierId = UUID.randomUUID();

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.builder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(customSavingsGoal)
            .lastModifiedBy(modifierId)
            .build();

    assertEquals(Currency.USD, dto.defaultCurrency());
    assertEquals(customSavingsGoal, dto.savingsGoal());
    assertEquals(modifierId, dto.lastModifiedBy());
  }

  @Test
  void defaultBuilder_shouldAllowOverridingDefaults() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.defaultBuilder(teamId)
            .lastModifiedBy(modifierId)
            .defaultCurrency(Currency.USD)
            .build();

    assertEquals(teamId, dto.teamId());
    assertEquals(modifierId, dto.lastModifiedBy());
    assertEquals(Currency.USD, dto.defaultCurrency());
  }

  @Test
  void record_shouldProvideCorrectAccessors() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    final LocalDateTime updatedAt = LocalDateTime.now();
    final BigDecimal savingsGoal = new BigDecimal("2500.00");
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("theme", "dark");

    final TeamPreferencesDTO dto =
        new TeamPreferencesDTO(
            teamId, Currency.COP, savingsGoal, metadata, modifierId, createdAt,
            updatedAt);

    assertEquals(teamId, dto.teamId());
    assertEquals(Currency.COP, dto.defaultCurrency());
    assertEquals(savingsGoal, dto.savingsGoal());
    assertEquals(metadata, dto.metadata());
    assertEquals(modifierId, dto.lastModifiedBy());
    assertEquals(createdAt, dto.createdAt());
    assertEquals(updatedAt, dto.updatedAt());
  }

  @Test
  void record_shouldImplementEqualsAndHashCode() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final LocalDateTime now = LocalDateTime.now();

    final TeamPreferencesDTO dto1 =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.COP)
            .savingsGoal(BigDecimal.ZERO)
            .metadata(PreferencesMetadata.empty())
            .lastModifiedBy(modifierId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    final TeamPreferencesDTO dto2 =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.COP)
            .savingsGoal(BigDecimal.ZERO)
            .metadata(PreferencesMetadata.empty())
            .lastModifiedBy(modifierId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(dto1, dto2);
    assertEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  void record_shouldImplementToString() {
    final UUID teamId = UUID.randomUUID();

    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.builder().teamId(teamId).build();

    final String toString = dto.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("TeamPreferencesDTO"));
    assertTrue(toString.contains(teamId.toString()));
  }
}

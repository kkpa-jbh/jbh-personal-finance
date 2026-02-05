package com.jbh.preferences.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamPreferencesDomainTest {

  @Test
  void createDefault_shouldCreateWithDefaultValues() {
    final UUID teamId = UUID.randomUUID();

    final TeamPreferencesDomain preferences = TeamPreferencesDomain.createDefault(teamId);

    assertEquals(teamId, preferences.getTeamId());
    assertEquals(Currency.COP, preferences.getDefaultCurrency());
    assertEquals(BigDecimal.ZERO.setScale(2), preferences.getSavingsGoal());
    assertTrue(preferences.getMetadata().isEmpty());
    assertNull(preferences.getLastModifiedBy());
    assertNotNull(preferences.getCreatedAt());
    assertNotNull(preferences.getUpdatedAt());
  }

  @Test
  void createDefault_shouldThrowExceptionForNullTeamId() {
    assertThrows(NullPointerException.class, () -> TeamPreferencesDomain.createDefault(null));
  }

  @Test
  void builder_shouldCreatePreferencesWithCustomValues() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();

    final TeamPreferencesDomain preferences = TeamPreferencesDomain.builder(teamId)
        .defaultCurrency(Currency.USD)
        .savingsGoal(new BigDecimal("5000.00"))
        .lastModifiedBy(modifierId)
        .build();

    assertEquals(teamId, preferences.getTeamId());
    assertEquals(Currency.USD, preferences.getDefaultCurrency());
    assertEquals(new BigDecimal("5000.00"), preferences.getSavingsGoal());
    assertEquals(modifierId, preferences.getLastModifiedBy());
  }

  @Test
  void builder_shouldThrowExceptionForNullTeamId() {
    assertThrows(NullPointerException.class, () -> TeamPreferencesDomain.builder(null));
  }

  @Test
  void withCurrency_shouldCreateNewInstanceWithUpdatedCurrency() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());

    final TeamPreferencesDomain updated = original.withCurrency(Currency.USD);

    assertNotSame(original, updated);
    assertEquals(Currency.USD, updated.getDefaultCurrency());
    assertEquals(original.getTeamId(), updated.getTeamId());
    assertEquals(original.getSavingsGoal(), updated.getSavingsGoal());
  }

  @Test
  void withCurrency_shouldThrowExceptionForNullCurrency() {
    final TeamPreferencesDomain preferences = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    assertThrows(NullPointerException.class, () -> preferences.withCurrency(null));
  }

  @Test
  void withSavingsGoal_shouldCreateNewInstanceWithUpdatedGoal() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    final BigDecimal newGoal = new BigDecimal("10000.00");

    final TeamPreferencesDomain updated = original.withSavingsGoal(newGoal);

    assertNotSame(original, updated);
    assertEquals(newGoal, updated.getSavingsGoal());
    assertEquals(original.getTeamId(), updated.getTeamId());
  }

  @Test
  void withSavingsGoal_shouldThrowExceptionForNegativeValue() {
    final TeamPreferencesDomain preferences = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    final BigDecimal negativeGoal = new BigDecimal("-100.00");

    assertThrows(IllegalArgumentException.class, () -> preferences.withSavingsGoal(negativeGoal));
  }

  @Test
  void withSavingsGoal_shouldAcceptNull() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());

    final TeamPreferencesDomain updated = original.withSavingsGoal(null);

    assertNotSame(original, updated);
    assertNull(updated.getSavingsGoal());
  }

  @Test
  void withMetadata_shouldCreateNewInstanceWithUpdatedMetadata() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    final PreferencesMetadata newMetadata = PreferencesMetadata.empty().with("key", "value");

    final TeamPreferencesDomain updated = original.withMetadata(newMetadata);

    assertNotSame(original, updated);
    assertEquals(newMetadata, updated.getMetadata());
  }

  @Test
  void withMetadata_shouldUseEmptyMetadataForNull() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());

    final TeamPreferencesDomain updated = original.withMetadata(null);

    assertNotSame(original, updated);
    assertTrue(updated.getMetadata().isEmpty());
  }

  @Test
  void withLastModifiedBy_shouldCreateNewInstanceWithUpdatedModifier() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    final UUID newModifier = UUID.randomUUID();

    final TeamPreferencesDomain updated = original.withLastModifiedBy(newModifier);

    assertNotSame(original, updated);
    assertEquals(newModifier, updated.getLastModifiedBy());
    assertEquals(original.getTeamId(), updated.getTeamId());
    assertEquals(original.getDefaultCurrency(), updated.getDefaultCurrency());
    assertEquals(original.getSavingsGoal(), updated.getSavingsGoal());
  }

  @Test
  void withLastModifiedBy_shouldAcceptNull() {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final TeamPreferencesDomain original = TeamPreferencesDomain.builder(teamId)
        .lastModifiedBy(modifierId)
        .build();

    final TeamPreferencesDomain updated = original.withLastModifiedBy(null);

    assertNotSame(original, updated);
    assertNull(updated.getLastModifiedBy());
  }

  @Test
  void withLastModifiedBy_shouldUpdateTimestamp() {
    final TeamPreferencesDomain original = TeamPreferencesDomain.createDefault(UUID.randomUUID());
    final UUID newModifier = UUID.randomUUID();

    final TeamPreferencesDomain updated = original.withLastModifiedBy(newModifier);

    assertNotEquals(original.getUpdatedAt(), updated.getUpdatedAt());
    assertTrue(updated.getUpdatedAt().isAfter(original.getUpdatedAt()) ||
               updated.getUpdatedAt().isEqual(original.getUpdatedAt()));
  }
}

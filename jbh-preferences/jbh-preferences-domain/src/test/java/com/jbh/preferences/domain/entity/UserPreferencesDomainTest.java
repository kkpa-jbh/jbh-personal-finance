package com.jbh.preferences.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserPreferencesDomainTest {

  @Test
  void createDefault_shouldCreateWithDefaultValues() {
    final UUID userId = UUID.randomUUID();

    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(userId);

    assertEquals(userId, preferences.getUserId());
    assertEquals(Language.DEFAULT, preferences.getDefaultLang());
    assertEquals(Currency.COP, preferences.getDefaultCurrency());
    assertEquals(BigDecimal.ZERO.setScale(2), preferences.getSavingsGoal());
    assertNull(preferences.getDefaultAccountId());
    assertTrue(preferences.getMetadata().isEmpty());
    assertNotNull(preferences.getCreatedAt());
    assertNotNull(preferences.getUpdatedAt());
  }

  @Test
  void createDefault_shouldThrowExceptionForNullUserId() {
    assertThrows(NullPointerException.class, () -> UserPreferencesDomain.createDefault(null));
  }

  @Test
  void builder_shouldCreatePreferencesWithCustomValues() {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();

    final UserPreferencesDomain preferences = UserPreferencesDomain.builder(userId)
        .defaultLang(Language.SPANISH)
        .defaultCurrency(Currency.COP)
        .savingsGoal(new BigDecimal("1000.00"))
        .defaultAccountId(accountId)
        .build();

    assertEquals(userId, preferences.getUserId());
    assertEquals(Language.SPANISH, preferences.getDefaultLang());
    assertEquals(Currency.COP, preferences.getDefaultCurrency());
    assertEquals(new BigDecimal("1000.00"), preferences.getSavingsGoal());
    assertEquals(accountId, preferences.getDefaultAccountId());
  }

  @Test
  void builder_shouldThrowExceptionForNullUserId() {
    assertThrows(NullPointerException.class, () -> UserPreferencesDomain.builder(null));
  }

  @Test
  void withLanguage_shouldCreateNewInstanceWithUpdatedLanguage() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());

    final UserPreferencesDomain updated = original.withLanguage(Language.SPANISH);

    assertNotSame(original, updated);
    assertEquals(Language.SPANISH, updated.getDefaultLang());
    assertEquals(original.getUserId(), updated.getUserId());
    assertEquals(original.getDefaultCurrency(), updated.getDefaultCurrency());
  }

  @Test
  void withLanguage_shouldThrowExceptionForNullLanguage() {
    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(UUID.randomUUID());
    assertThrows(NullPointerException.class, () -> preferences.withLanguage(null));
  }

  @Test
  void withCurrency_shouldCreateNewInstanceWithUpdatedCurrency() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());

    final UserPreferencesDomain updated = original.withCurrency(Currency.COP);

    assertNotSame(original, updated);
    assertEquals(Currency.COP, updated.getDefaultCurrency());
    assertEquals(original.getUserId(), updated.getUserId());
    assertEquals(original.getDefaultLang(), updated.getDefaultLang());
  }

  @Test
  void withCurrency_shouldThrowExceptionForNullCurrency() {
    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(UUID.randomUUID());
    assertThrows(NullPointerException.class, () -> preferences.withCurrency(null));
  }

  @Test
  void withSavingsGoal_shouldCreateNewInstanceWithUpdatedGoal() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());
    final BigDecimal newGoal = new BigDecimal("5000.00");

    final UserPreferencesDomain updated = original.withSavingsGoal(newGoal);

    assertNotSame(original, updated);
    assertEquals(newGoal, updated.getSavingsGoal());
  }

  @Test
  void withSavingsGoal_shouldThrowExceptionForNegativeGoal() {
    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(UUID.randomUUID());
    final BigDecimal negativeGoal = new BigDecimal("-100.00");
    assertThrows(IllegalArgumentException.class, () -> preferences.withSavingsGoal(negativeGoal));
  }

  @Test
  void withSavingsGoal_shouldAllowNullGoal() {
    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(UUID.randomUUID());

    final UserPreferencesDomain updated = preferences.withSavingsGoal(null);

    assertNull(updated.getSavingsGoal());
  }

  @Test
  void withSavingsGoal_shouldAllowZeroGoal() {
    final UserPreferencesDomain preferences = UserPreferencesDomain.createDefault(UUID.randomUUID());

    final UserPreferencesDomain updated = preferences.withSavingsGoal(BigDecimal.ZERO);

    assertEquals(BigDecimal.ZERO, updated.getSavingsGoal());
  }

  @Test
  void withDefaultAccountId_shouldCreateNewInstanceWithUpdatedAccountId() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());
    final UUID newAccountId = UUID.randomUUID();

    final UserPreferencesDomain updated = original.withDefaultAccountId(newAccountId);

    assertNotSame(original, updated);
    assertEquals(newAccountId, updated.getDefaultAccountId());
  }

  @Test
  void withDefaultAccountId_shouldAllowNullAccountId() {
    final UUID accountId = UUID.randomUUID();
    final UserPreferencesDomain original = UserPreferencesDomain.builder(UUID.randomUUID())
        .defaultAccountId(accountId)
        .build();

    final UserPreferencesDomain updated = original.withDefaultAccountId(null);

    assertNull(updated.getDefaultAccountId());
  }

  @Test
  void withMetadata_shouldCreateNewInstanceWithUpdatedMetadata() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());
    final PreferencesMetadata newMetadata = PreferencesMetadata.empty().with("theme", "dark");

    final UserPreferencesDomain updated = original.withMetadata(newMetadata);

    assertNotSame(original, updated);
    assertEquals("dark", updated.getMetadata().get("theme"));
  }

  @Test
  void withMetadata_shouldHandleNullMetadata() {
    final UserPreferencesDomain original = UserPreferencesDomain.createDefault(UUID.randomUUID());

    final UserPreferencesDomain updated = original.withMetadata(null);

    assertTrue(updated.getMetadata().isEmpty());
  }
}

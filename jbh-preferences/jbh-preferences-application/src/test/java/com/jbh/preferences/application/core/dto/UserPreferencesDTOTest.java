package com.jbh.preferences.application.core.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesId;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserPreferencesDTOTest {

  @Test
  void builder_shouldCreateDTOWithDefaultValues() {
    final UUID userId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final LocalDateTime now = LocalDateTime.now();

    final UserPreferencesDTO dto =
        UserPreferencesDTO.builder()
            .id(preferencesId)
            .userId(userId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(preferencesId, dto.id());
    assertEquals(userId, dto.userId());
    assertEquals(Language.DEFAULT, dto.defaultLang());
    assertEquals(Currency.defaultCurrency(), dto.defaultCurrency());
    assertEquals(JbhMoneyUtils.JBH_ZERO, dto.savingsGoal());
    assertTrue(dto.metadata().isEmpty());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void defaultBuilder_shouldCreateDTOWithUserIdAndDefaults() {
    final UUID userId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final LocalDateTime now = LocalDateTime.now();

    final UserPreferencesDTO dto =
        UserPreferencesDTO.defaultBuilder(userId)
            .id(preferencesId)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(preferencesId, dto.id());
    assertEquals(userId, dto.userId());
    assertEquals(Language.DEFAULT, dto.defaultLang());
    assertEquals(Currency.defaultCurrency(), dto.defaultCurrency());
    assertEquals(JbhMoneyUtils.JBH_ZERO, dto.savingsGoal());
    assertTrue(dto.metadata().isEmpty());
  }

  @Test
  void internalBuilder_shouldAllowFullCustomization() {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final LocalDateTime now = LocalDateTime.now();
    final BigDecimal savingsGoal = new BigDecimal("1000.50");
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("key", "value");

    final UserPreferencesDTO dto =
        UserPreferencesDTO.internalBuilder()
            .id(preferencesId)
            .userId(userId)
            .defaultLang(Language.ENGLISH)
            .defaultCurrency(Currency.USD)
            .savingsGoal(savingsGoal)
            .defaultAccountId(accountId)
            .metadata(metadata)
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(preferencesId, dto.id());
    assertEquals(userId, dto.userId());
    assertEquals(Language.ENGLISH, dto.defaultLang());
    assertEquals(Currency.USD, dto.defaultCurrency());
    assertEquals(savingsGoal, dto.savingsGoal());
    assertEquals(accountId, dto.defaultAccountId());
    assertEquals(metadata, dto.metadata());
    assertEquals(now, dto.createdAt());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void builder_shouldAllowOverridingDefaults() {
    final UUID userId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final BigDecimal customSavingsGoal = new BigDecimal("5000.00");

    final UserPreferencesDTO dto =
        UserPreferencesDTO.builder()
            .id(preferencesId)
            .userId(userId)
            .defaultLang(Language.ENGLISH)
            .defaultCurrency(Currency.USD)
            .savingsGoal(customSavingsGoal)
            .build();

    assertEquals(Language.ENGLISH, dto.defaultLang());
    assertEquals(Currency.USD, dto.defaultCurrency());
    assertEquals(customSavingsGoal, dto.savingsGoal());
  }

  @Test
  void defaultBuilder_shouldAllowOverridingDefaults() {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();

    final UserPreferencesDTO dto =
        UserPreferencesDTO.defaultBuilder(userId)
            .id(preferencesId)
            .defaultAccountId(accountId)
            .defaultLang(Language.ENGLISH)
            .build();

    assertEquals(userId, dto.userId());
    assertEquals(accountId, dto.defaultAccountId());
    assertEquals(Language.ENGLISH, dto.defaultLang());
  }

  @Test
  void record_shouldProvideCorrectAccessors() {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    final LocalDateTime updatedAt = LocalDateTime.now();
    final BigDecimal savingsGoal = new BigDecimal("2500.00");
    final PreferencesMetadata metadata = PreferencesMetadata.empty().with("theme", "dark");

    final UserPreferencesDTO dto =
        new UserPreferencesDTO(
            preferencesId,
            userId,
            Language.SPANISH,
            Currency.COP,
            savingsGoal,
            accountId,
            metadata,
            createdAt,
            updatedAt);

    assertEquals(preferencesId, dto.id());
    assertEquals(userId, dto.userId());
    assertEquals(Language.SPANISH, dto.defaultLang());
    assertEquals(Currency.COP, dto.defaultCurrency());
    assertEquals(savingsGoal, dto.savingsGoal());
    assertEquals(accountId, dto.defaultAccountId());
    assertEquals(metadata, dto.metadata());
    assertEquals(createdAt, dto.createdAt());
    assertEquals(updatedAt, dto.updatedAt());
  }

  @Test
  void record_shouldImplementEqualsAndHashCode() {
    final UUID userId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();
    final LocalDateTime now = LocalDateTime.now();

    final UserPreferencesDTO dto1 =
        UserPreferencesDTO.internalBuilder()
            .id(preferencesId)
            .userId(userId)
            .defaultLang(Language.SPANISH)
            .defaultCurrency(Currency.COP)
            .savingsGoal(BigDecimal.ZERO)
            .metadata(PreferencesMetadata.empty())
            .createdAt(now)
            .updatedAt(now)
            .build();

    final UserPreferencesDTO dto2 =
        UserPreferencesDTO.internalBuilder()
            .id(preferencesId)
            .userId(userId)
            .defaultLang(Language.SPANISH)
            .defaultCurrency(Currency.COP)
            .savingsGoal(BigDecimal.ZERO)
            .metadata(PreferencesMetadata.empty())
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertEquals(dto1, dto2);
    assertEquals(dto1.hashCode(), dto2.hashCode());
  }

  @Test
  void record_shouldImplementToString() {
    final UUID userId = UUID.randomUUID();
    final PreferencesId preferencesId = PreferencesId.generate();

    final UserPreferencesDTO dto =
        UserPreferencesDTO.builder().id(preferencesId).userId(userId).build();

    final String toString = dto.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("UserPreferencesDTO"));
    assertTrue(toString.contains(userId.toString()));
  }
}

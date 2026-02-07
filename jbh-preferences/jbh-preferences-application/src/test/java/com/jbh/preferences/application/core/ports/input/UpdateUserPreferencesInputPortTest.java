package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.services.PreferencesService;
import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUserPreferencesInputPortTest {

  @Mock private PreferencesService preferencesService;

  private UpdateUserPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new UpdateUserPreferencesInputPort(preferencesService);
  }

  @Test
  void execute_shouldDelegateToPreferencesService() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UpdatePreferencesCommand command = UpdatePreferencesCommand.builder().build();
    final UserPreferencesDTO expectedDto = createSampleDTO(userId);
    when(preferencesService.updatePreferences(userId, command)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId, command);

    assertEquals(expectedDto, result);
    verify(preferencesService).updatePreferences(userId, command);
  }

  private UserPreferencesDTO createSampleDTO(final UUID userId) {
    return UserPreferencesDTO.internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .metadata(PreferencesMetadata.empty())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldReturnUpdatedDTOWithNewValues() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final UpdatePreferencesCommand command =
        UpdatePreferencesCommand.builder()
            .defaultLang(Language.ENGLISH)
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("3000.00"))
            .defaultAccountId(accountId)
            .build();
    final UserPreferencesDTO expectedDto = createUpdatedDTO(userId, accountId);
    when(preferencesService.updatePreferences(userId, command)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId, command);

    assertEquals(userId, result.userId());
    assertEquals(Language.ENGLISH, result.defaultLang());
    assertEquals(Currency.USD, result.defaultCurrency());
    assertEquals(new BigDecimal("3000.00"), result.savingsGoal());
    assertEquals(accountId, result.defaultProductId());
  }

  private UserPreferencesDTO createUpdatedDTO(final UUID userId, final UUID accountId) {
    return UserPreferencesDTO.internalBuilder()
        .userId(userId)
        .defaultLang(Language.ENGLISH)
        .defaultCurrency(Currency.USD)
        .savingsGoal(new BigDecimal("3000.00"))
        .defaultProductId(accountId)
        .metadata(PreferencesMetadata.empty())
        .createdAt(LocalDateTime.now().minusDays(1))
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UpdatePreferencesCommand command = UpdatePreferencesCommand.builder().build();
    final BusinessException expectedException =
        new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND);
    when(preferencesService.updatePreferences(userId, command)).thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(userId, command));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(preferencesService).updatePreferences(userId, command);
  }

  @Test
  void execute_shouldPropagateNullPointerExceptionForNullUserId() throws BusinessException {
    final UpdatePreferencesCommand command = UpdatePreferencesCommand.builder().build();
    when(preferencesService.updatePreferences(null, command)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null, command));
    verify(preferencesService).updatePreferences(null, command);
  }

  @Test
  void execute_shouldPropagateNullPointerExceptionForNullCommand() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    when(preferencesService.updatePreferences(userId, null)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(userId, null));
    verify(preferencesService).updatePreferences(userId, null);
  }

  @Test
  void execute_shouldUpdatePartialFields() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UpdatePreferencesCommand command =
        UpdatePreferencesCommand.builder().savingsGoal(new BigDecimal("5000.00")).build();
    final UserPreferencesDTO expectedDto = createPartiallyUpdatedDTO(userId);
    when(preferencesService.updatePreferences(userId, command)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId, command);

    assertEquals(Language.DEFAULT, result.defaultLang());
    assertEquals(Currency.defaultCurrency(), result.defaultCurrency());
    assertEquals(new BigDecimal("5000.00"), result.savingsGoal());
  }

  private UserPreferencesDTO createPartiallyUpdatedDTO(final UUID userId) {
    return UserPreferencesDTO.internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(new BigDecimal("5000.00"))
        .metadata(PreferencesMetadata.empty())
        .createdAt(LocalDateTime.now().minusDays(1))
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldUpdateMetadata() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final PreferencesMetadata metadata =
        PreferencesMetadata.empty().with("theme", "dark").with("notifications", true);
    final UpdatePreferencesCommand command =
        UpdatePreferencesCommand.builder().metadata(metadata).build();
    final UserPreferencesDTO expectedDto = createDTOWithMetadata(userId, metadata);
    when(preferencesService.updatePreferences(userId, command)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId, command);

    assertEquals(metadata, result.metadata());
  }

  private UserPreferencesDTO createDTOWithMetadata(
      final UUID userId, final PreferencesMetadata metadata) {
    return UserPreferencesDTO.internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .metadata(metadata)
        .createdAt(LocalDateTime.now().minusDays(1))
        .updatedAt(LocalDateTime.now())
        .build();
  }
}

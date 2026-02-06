package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.services.PreferencesService;
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
class GetUserPreferencesInputPortTest {

  @Mock private PreferencesService preferencesService;

  private GetUserPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new GetUserPreferencesInputPort(preferencesService);
  }

  @Test
  void execute_shouldDelegateToPreferencesService() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO expectedDto = createSampleDTO(userId);
    when(preferencesService.getPreferences(userId)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId);

    assertEquals(expectedDto, result);
    verify(preferencesService).getPreferences(userId);
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
  void execute_shouldReturnDTOWithAllFields() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    final UserPreferencesDTO expectedDto = createCustomDTO(userId, accountId);
    when(preferencesService.getPreferences(userId)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId);

    assertEquals(userId, result.userId());
    assertEquals(Language.ENGLISH, result.defaultLang());
    assertEquals(Currency.USD, result.defaultCurrency());
    assertEquals(new BigDecimal("1500.00"), result.savingsGoal());
    assertEquals(accountId, result.defaultProductId());
    assertNotNull(result.metadata());
  }

  private UserPreferencesDTO createCustomDTO(final UUID userId, final UUID accountId) {
    return UserPreferencesDTO.internalBuilder()
        .userId(userId)
        .defaultLang(Language.ENGLISH)
        .defaultCurrency(Currency.USD)
        .savingsGoal(new BigDecimal("1500.00"))
        .defaultAccountId(accountId)
        .metadata(PreferencesMetadata.empty().with("theme", "dark"))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final BusinessException expectedException =
        new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_ALREADY_EXIST);
    when(preferencesService.getPreferences(userId)).thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(userId));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(preferencesService).getPreferences(userId);
  }

  @Test
  void execute_shouldPropagateNullPointerException() throws BusinessException {
    when(preferencesService.getPreferences(null)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null));
    verify(preferencesService).getPreferences(null);
  }

  @Test
  void execute_shouldHandleDefaultPreferencesCreation() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO defaultDto = createSampleDTO(userId);
    when(preferencesService.getPreferences(userId)).thenReturn(defaultDto);

    final UserPreferencesDTO result = inputPort.execute(userId);

    assertNotNull(result);
    assertEquals(userId, result.userId());
    assertEquals(Language.DEFAULT, result.defaultLang());
    assertEquals(Currency.defaultCurrency(), result.defaultCurrency());
  }
}

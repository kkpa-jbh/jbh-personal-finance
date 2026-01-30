package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.services.PreferencesService;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateUserPreferencesInputPortTest {

  @Mock private PreferencesService preferencesService;

  private CreateUserPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new CreateUserPreferencesInputPort(preferencesService);
  }

  @Test
  void execute_shouldDelegateToPreferencesService() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO expectedDto = createSampleDTO(userId);
    when(preferencesService.createDefaultPreferences(userId)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId);

    assertEquals(expectedDto, result);
    verify(preferencesService).createDefaultPreferences(userId);
  }

  private UserPreferencesDTO createSampleDTO(final UUID userId) {
    return UserPreferencesDTO.internalBuilder()
        .id(PreferencesId.generate())
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldReturnDTOWithCorrectValues() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO expectedDto = createSampleDTO(userId);
    when(preferencesService.createDefaultPreferences(userId)).thenReturn(expectedDto);

    final UserPreferencesDTO result = inputPort.execute(userId);

    assertEquals(userId, result.userId());
    assertEquals(Language.DEFAULT, result.defaultLang());
    assertEquals(Currency.defaultCurrency(), result.defaultCurrency());
    assertEquals(BigDecimal.ZERO, result.savingsGoal());
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final BusinessException expectedException =
        new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_ALREADY_EXIST);
    when(preferencesService.createDefaultPreferences(userId)).thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(userId));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(preferencesService).createDefaultPreferences(userId);
  }

  @Test
  void execute_shouldPropagateNullPointerException() throws BusinessException {
    when(preferencesService.createDefaultPreferences(null)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null));
    verify(preferencesService).createDefaultPreferences(null);
  }
}

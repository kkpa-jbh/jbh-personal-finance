package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import com.jbh.preferences.domain.vo.Currency;
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
class GetTeamPreferencesInputPortTest {

  @Mock private TeamPreferencesService preferencesService;

  private GetTeamPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new GetTeamPreferencesInputPort(preferencesService);
  }

  @Test
  void execute_shouldDelegateToPreferencesService() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final TeamPreferencesDTO expectedDto = createSampleDTO(teamId);
    when(preferencesService.getPreferences(teamId)).thenReturn(expectedDto);

    final TeamPreferencesDTO result = inputPort.execute(teamId);

    assertEquals(expectedDto, result);
    verify(preferencesService).getPreferences(teamId);
  }

  private TeamPreferencesDTO createSampleDTO(final UUID teamId) {
    return TeamPreferencesDTO.internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .metadata(PreferencesMetadata.empty())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final BusinessException expectedException =
        new BusinessException(PreferencesBusinessExceptionType.TEAM_PREFERENCES_NOT_FOUND);
    when(preferencesService.getPreferences(teamId)).thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(teamId));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(preferencesService).getPreferences(teamId);
  }

  @Test
  void execute_shouldPropagateNullPointerException() throws BusinessException {
    when(preferencesService.getPreferences(null)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null));
    verify(preferencesService).getPreferences(null);
  }
}

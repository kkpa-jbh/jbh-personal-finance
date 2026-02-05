package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
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
class UpdateTeamPreferencesInputPortTest {

  @Mock private TeamPreferencesService preferencesService;

  private UpdateTeamPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new UpdateTeamPreferencesInputPort(preferencesService);
  }

  @Test
  void execute_shouldDelegateToPreferencesService() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final UpdateTeamPreferencesCommand command =
        UpdateTeamPreferencesCommand.builder()
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("5000.00"))
            .lastModifiedBy(modifierId)
            .build();
    final TeamPreferencesDTO expectedDto = createUpdatedDTO(teamId, modifierId);

    when(preferencesService.updatePreferences(teamId, command)).thenReturn(expectedDto);

    final TeamPreferencesDTO result = inputPort.execute(teamId, command);

    assertEquals(expectedDto, result);
    assertEquals(modifierId, result.lastModifiedBy());
    verify(preferencesService).updatePreferences(teamId, command);
  }

  private TeamPreferencesDTO createUpdatedDTO(final UUID teamId, final UUID modifierId) {
    return TeamPreferencesDTO.internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.USD)
        .savingsGoal(new BigDecimal("5000.00"))
        .metadata(PreferencesMetadata.empty())
        .lastModifiedBy(modifierId)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UpdateTeamPreferencesCommand command = UpdateTeamPreferencesCommand.builder().build();
    final BusinessException expectedException =
        new BusinessException(
            com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType
                .TEAM_PREFERENCES_NOT_FOUND);

    when(preferencesService.updatePreferences(teamId, command)).thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(teamId, command));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(preferencesService).updatePreferences(teamId, command);
  }

  @Test
  void execute_shouldPropagateNullPointerException() throws BusinessException {
    when(preferencesService.updatePreferences(null, null)).thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null, null));
    verify(preferencesService).updatePreferences(null, null);
  }

  @Test
  void execute_shouldPassLastModifiedByFromCommand() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierId = UUID.randomUUID();
    final UpdateTeamPreferencesCommand command =
        UpdateTeamPreferencesCommand.builder().lastModifiedBy(modifierId).build();
    final TeamPreferencesDTO expectedDto = createUpdatedDTO(teamId, modifierId);

    when(preferencesService.updatePreferences(teamId, command)).thenReturn(expectedDto);

    final TeamPreferencesDTO result = inputPort.execute(teamId, command);

    assertEquals(modifierId, result.lastModifiedBy());
    verify(preferencesService).updatePreferences(teamId, command);
  }
}

package com.jbh.preferences.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import com.jbh.preferences.domain.vo.Currency;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateTeamPreferencesInputPortTest {

  @Mock private TeamPreferencesService teamPreferencesService;

  private CreateTeamPreferencesInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new CreateTeamPreferencesInputPort(teamPreferencesService);
  }

  @Test
  void execute_shouldDelegateToTeamPreferencesService() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID creatorUserId = UUID.randomUUID();
    final TeamPreferencesDTO expectedDto = createSampleDTO(teamId, creatorUserId);
    when(teamPreferencesService.createDefaultPreferences(teamId, creatorUserId))
        .thenReturn(expectedDto);

    final TeamPreferencesDTO result = inputPort.execute(teamId, creatorUserId);

    assertEquals(expectedDto, result);
    verify(teamPreferencesService).createDefaultPreferences(teamId, creatorUserId);
  }

  @Test
  void execute_shouldReturnDTOWithCorrectValues() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID creatorUserId = UUID.randomUUID();
    final TeamPreferencesDTO expectedDto = createSampleDTO(teamId, creatorUserId);
    when(teamPreferencesService.createDefaultPreferences(teamId, creatorUserId))
        .thenReturn(expectedDto);

    final TeamPreferencesDTO result = inputPort.execute(teamId, creatorUserId);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(Currency.COP, result.defaultCurrency());
    assertEquals(JbhMoneyUtils.JBH_ZERO, result.savingsGoal());
    assertEquals(creatorUserId, result.lastModifiedBy());
  }

  @Test
  void execute_shouldPropagateBusinessException() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID creatorUserId = UUID.randomUUID();
    final BusinessException expectedException =
        new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_ALREADY_EXIST);
    when(teamPreferencesService.createDefaultPreferences(teamId, creatorUserId))
        .thenThrow(expectedException);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> inputPort.execute(teamId, creatorUserId));

    assertEquals(expectedException.getMessage(), exception.getMessage());
    verify(teamPreferencesService).createDefaultPreferences(teamId, creatorUserId);
  }

  @Test
  void execute_shouldPropagateNullPointerExceptionForNullTeamId() throws BusinessException {
    final UUID creatorUserId = UUID.randomUUID();
    when(teamPreferencesService.createDefaultPreferences(null, creatorUserId))
        .thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(null, creatorUserId));
    verify(teamPreferencesService).createDefaultPreferences(null, creatorUserId);
  }

  @Test
  void execute_shouldPropagateNullPointerExceptionForNullCreatorUserId() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    when(teamPreferencesService.createDefaultPreferences(teamId, null))
        .thenThrow(NullPointerException.class);

    assertThrows(NullPointerException.class, () -> inputPort.execute(teamId, null));
    verify(teamPreferencesService).createDefaultPreferences(teamId, null);
  }

  private TeamPreferencesDTO createSampleDTO(final UUID teamId, final UUID creatorUserId) {
    return TeamPreferencesDTO.defaultBuilder(teamId)
        .lastModifiedBy(creatorUserId)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}

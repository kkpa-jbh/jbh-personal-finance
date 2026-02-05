package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import java.util.UUID;

public class UpdateTeamPreferencesInputPort {

  private final TeamPreferencesService preferencesService;

  public UpdateTeamPreferencesInputPort(final TeamPreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public TeamPreferencesDTO execute(final UUID teamId, final UpdateTeamPreferencesCommand command)
      throws BusinessException {
    return preferencesService.updatePreferences(teamId, command);
  }
}

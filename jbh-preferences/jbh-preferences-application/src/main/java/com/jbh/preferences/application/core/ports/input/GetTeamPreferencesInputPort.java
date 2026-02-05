package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import java.util.UUID;

public class GetTeamPreferencesInputPort {

  private final TeamPreferencesService preferencesService;

  public GetTeamPreferencesInputPort(final TeamPreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public TeamPreferencesDTO execute(final UUID teamId) throws BusinessException {
    return preferencesService.getPreferences(teamId);
  }
}

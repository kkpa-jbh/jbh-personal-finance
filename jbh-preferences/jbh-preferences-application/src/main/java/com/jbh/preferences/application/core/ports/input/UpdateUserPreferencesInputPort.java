package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.services.PreferencesService;
import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import java.util.UUID;

public class UpdateUserPreferencesInputPort {

  private final PreferencesService preferencesService;

  public UpdateUserPreferencesInputPort(final PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public UserPreferencesDTO execute(final UUID userId, final UpdatePreferencesCommand command)
      throws BusinessException {
    return preferencesService.updatePreferences(userId, command);
  }
}

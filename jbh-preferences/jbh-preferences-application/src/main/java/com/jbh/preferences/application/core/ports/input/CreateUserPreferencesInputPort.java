package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.services.PreferencesService;
import java.util.UUID;

public class CreateUserPreferencesInputPort {

  private final PreferencesService preferencesService;

  public CreateUserPreferencesInputPort(final PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public UserPreferencesDTO execute(final UUID userId) throws BusinessException {
    return preferencesService.createDefaultPreferences(userId);
  }
}

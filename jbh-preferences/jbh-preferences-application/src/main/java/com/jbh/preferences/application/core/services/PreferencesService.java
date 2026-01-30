package com.jbh.preferences.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import java.util.UUID;

public interface PreferencesService {

  UserPreferencesDTO getPreferences(UUID userId) throws BusinessException;

  UserPreferencesDTO createDefaultPreferences(UUID userId) throws BusinessException;

  UserPreferencesDTO updatePreferences(UUID userId, UpdatePreferencesCommand command)
      throws BusinessException;

  UserPreferencesDTO getOrCreatePreferences(UUID userId) throws BusinessException;
}

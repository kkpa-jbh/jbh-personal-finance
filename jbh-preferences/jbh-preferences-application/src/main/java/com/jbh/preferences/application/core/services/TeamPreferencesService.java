package com.jbh.preferences.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import java.util.UUID;

public interface TeamPreferencesService {

  TeamPreferencesDTO getPreferences(UUID teamId) throws BusinessException;

  TeamPreferencesDTO createDefaultPreferences(UUID teamId, UUID creatorUserId)
      throws BusinessException;

  TeamPreferencesDTO updatePreferences(UUID teamId, UpdateTeamPreferencesCommand command)
      throws BusinessException;

  TeamPreferencesDTO getOrCreatePreferences(UUID teamId, UUID userId) throws BusinessException;
}

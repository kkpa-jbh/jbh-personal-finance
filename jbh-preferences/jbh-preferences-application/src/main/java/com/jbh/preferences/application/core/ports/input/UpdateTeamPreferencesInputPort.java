package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamMembershipGuard;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.application.core.vo.CallerToken;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import java.util.UUID;

public class UpdateTeamPreferencesInputPort {

  private final TeamPreferencesService preferencesService;
  private final TeamMembershipGuard membershipGuard;

  public UpdateTeamPreferencesInputPort(
      final TeamPreferencesService preferencesService, final TeamMembershipGuard membershipGuard) {
    this.preferencesService = preferencesService;
    this.membershipGuard = membershipGuard;
  }

  /** Only a member of the team can change its preferences. */
  public TeamPreferencesDTO execute(
      final UUID teamId, final UpdateTeamPreferencesCommand command, final CallerToken caller)
      throws BusinessException, InternalSystemException {
    membershipGuard.requireMember(teamId, caller);
    return preferencesService.updatePreferences(teamId, command);
  }
}

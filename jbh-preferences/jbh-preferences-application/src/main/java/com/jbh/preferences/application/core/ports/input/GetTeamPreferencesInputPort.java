package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamMembershipGuard;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import com.jbh.preferences.application.core.vo.CallerToken;
import java.util.UUID;

public class GetTeamPreferencesInputPort {

  private final TeamPreferencesService preferencesService;
  private final TeamMembershipGuard membershipGuard;

  public GetTeamPreferencesInputPort(
      final TeamPreferencesService preferencesService, final TeamMembershipGuard membershipGuard) {
    this.preferencesService = preferencesService;
    this.membershipGuard = membershipGuard;
  }

  /**
   * For trusted calls inside this service, with no user request (the intermodule lookup). It does
   * not check membership. User requests must use {@link #execute(UUID, CallerToken)}.
   */
  public TeamPreferencesDTO execute(final UUID teamId) throws BusinessException {
    return preferencesService.getPreferences(teamId);
  }

  /** For user requests. Only a member of the team gets its preferences. */
  public TeamPreferencesDTO execute(final UUID teamId, final CallerToken caller)
      throws BusinessException, InternalSystemException {
    membershipGuard.requireMember(teamId, caller);
    return preferencesService.getPreferences(teamId);
  }
}

package com.jbh.preferences.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.preferences.application.core.ports.output.TeamMembershipQueryRepository;
import com.jbh.preferences.application.core.vo.CallerToken;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import java.util.Objects;
import java.util.UUID;

/** Lets only the members of a team use its preferences. */
public class TeamMembershipGuard {

  private static final String TEAM_ID_NULL_MSG = "Team ID cannot be null";
  private static final String CALLER_NULL_MSG = "Caller token cannot be null";

  private final TeamMembershipQueryRepository membershipRepository;

  public TeamMembershipGuard(final TeamMembershipQueryRepository membershipRepository) {
    this.membershipRepository = membershipRepository;
  }

  /**
   * Stops a caller who is not a member of the team. The error says "team not found", so a
   * non-member cannot learn if the team exists.
   *
   * @throws BusinessException TEAM_NOT_FOUND when the caller is not a member
   * @throws InternalSystemException when IAM does not accept the token
   */
  public void requireMember(final UUID teamId, final CallerToken caller)
      throws BusinessException, InternalSystemException {
    Objects.requireNonNull(teamId, TEAM_ID_NULL_MSG);
    Objects.requireNonNull(caller, CALLER_NULL_MSG);
    if (!membershipRepository.isMember(teamId, caller)) {
      throw new BusinessException(PreferencesBusinessExceptionType.TEAM_NOT_FOUND, teamId);
    }
  }
}

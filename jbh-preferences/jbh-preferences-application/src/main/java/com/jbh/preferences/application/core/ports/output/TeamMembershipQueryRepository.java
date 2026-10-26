package com.jbh.preferences.application.core.ports.output;

import com.jbh.commons.exception.InternalSystemException;
import com.jbh.preferences.application.core.vo.CallerToken;
import java.util.UUID;

/** Asks IAM if the caller is an accepted member of a team. */
public interface TeamMembershipQueryRepository {

  /**
   * Checks the membership of the caller in the team.
   *
   * @param teamId the team (an IAM user group)
   * @param caller the token of the caller
   * @return true when the caller is an accepted member, false when not or when the team does not
   *     exist
   * @throws InternalSystemException when IAM does not accept the token
   */
  boolean isMember(UUID teamId, CallerToken caller) throws InternalSystemException;
}

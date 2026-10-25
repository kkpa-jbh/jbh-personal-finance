package com.jbh.preferences.application.core.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.InternalSystemException;
import com.jbh.preferences.application.core.vo.CallerToken;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamMembershipGuardTest {

  private static final CallerToken CALLER = new CallerToken("Bearer user");
  private static final UUID TEAM_ID = UUID.randomUUID();

  @Test
  void shouldLetAMemberPass() {
    final TeamMembershipGuard guard =
        new TeamMembershipGuard((team, caller) -> TEAM_ID.equals(team) && CALLER.equals(caller));

    assertDoesNotThrow(() -> guard.requireMember(TEAM_ID, CALLER));
  }

  @Test
  void shouldAnswerTeamNotFoundToANonMember() {
    final TeamMembershipGuard guard = new TeamMembershipGuard((team, caller) -> false);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> guard.requireMember(TEAM_ID, CALLER));

    assertEquals(
        PreferencesBusinessExceptionType.TEAM_NOT_FOUND, exception.getBusinessExceptionType());
  }

  @Test
  void shouldPassOnTheErrorWhenIamDoesNotAcceptTheToken() {
    final InternalSystemException unauthenticated = new InternalSystemException("Unauthenticated");
    final TeamMembershipGuard guard =
        new TeamMembershipGuard(
            (team, caller) -> {
              throw unauthenticated;
            });

    final InternalSystemException exception =
        assertThrows(InternalSystemException.class, () -> guard.requireMember(TEAM_ID, CALLER));

    assertEquals(unauthenticated, exception);
  }

  @Test
  void shouldThrowNullPointerExceptionForANullTeamOrCaller() {
    final TeamMembershipGuard guard = new TeamMembershipGuard((team, caller) -> true);

    assertThrows(NullPointerException.class, () -> guard.requireMember(null, CALLER));
    assertThrows(NullPointerException.class, () -> guard.requireMember(TEAM_ID, null));
  }
}

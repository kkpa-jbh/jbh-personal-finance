package com.jbh.notification.infra.testfixtures.fakes;

import com.jbh.preferences.contracts.PreferencesLookupPort;
import com.jbh.preferences.contracts.TeamPreferencesData;
import com.jbh.preferences.contracts.UserPreferencesData;
import io.quarkus.test.Mock;
import java.util.Optional;
import java.util.UUID;

/**
 * Stands in for the preferences module in {@code @QuarkusTest}, because this module alone has no
 * implementation of the port. The migration test sends no notification, so no method is called.
 */
@Mock
public class FakePreferencesLookupPort implements PreferencesLookupPort {

  @Override
  public UserPreferencesData getPreferences(final UUID userId) {
    return UserPreferencesData.defaultFor(userId);
  }

  @Override
  public Optional<UserPreferencesData> findPreferences(final UUID userId) {
    return Optional.of(UserPreferencesData.defaultFor(userId));
  }

  @Override
  public TeamPreferencesData getTeamPreferences(final UUID teamId) {
    throw new UnsupportedOperationException("Not used by the migration test");
  }

  @Override
  public Optional<TeamPreferencesData> findTeamPreferences(final UUID teamId) {
    return Optional.empty();
  }
}

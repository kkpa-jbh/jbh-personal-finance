package com.jbh.notification.infra.ports.output;

import com.jbh.preferences.contracts.PreferencesLookupException;
import com.jbh.preferences.contracts.UserPreferencesData;
import java.util.UUID;

/**
 * Output port for fetching user preferences from the preferences module.
 * This interface allows the notification module to access user preferences
 * without direct dependency on the preferences implementation.
 */
public interface UserPreferencesPort {

  String DEFAULT_LANGUAGE = "es";

  /**
   * Retrieves user preferences for the specified user.
   *
   * @param userId the user ID to look up preferences for
   * @return the user preferences data
   * @throws PreferencesLookupException if the preferences cannot be retrieved
   */
  UserPreferencesData getUserPreferences(UUID userId) throws PreferencesLookupException;

  /**
   * Retrieves the language code for a user, falling back to default if not found.
   *
   * @param userId the user ID to look up
   * @return the language code (e.g., "en", "es")
   */
  default String getUserLanguage(final UUID userId) {
    try {
      return getUserPreferences(userId).languageCode();
    } catch (final PreferencesLookupException e) {
      return DEFAULT_LANGUAGE;
    }
  }
}

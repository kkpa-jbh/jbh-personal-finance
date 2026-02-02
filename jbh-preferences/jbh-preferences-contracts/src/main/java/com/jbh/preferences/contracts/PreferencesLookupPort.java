package com.jbh.preferences.contracts;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for looking up user preferences.
 * This interface enables inter-module communication without HTTP overhead.
 */
public interface PreferencesLookupPort {

  String DEFAULT_LANGUAGE_CODE = "es";

  /**
   * Retrieves user preferences for the specified user.
   *
   * @param userId the user ID to look up preferences for
   * @return the user preferences data
   * @throws PreferencesLookupException if the preferences cannot be retrieved
   */
  UserPreferencesData getPreferences(UUID userId) throws PreferencesLookupException;

  /**
   * Finds user preferences if they exist.
   *
   * @param userId the user ID to look up preferences for
   * @return an Optional containing the preferences if found, empty otherwise
   * @throws PreferencesLookupException if an error occurs during lookup
   */
  Optional<UserPreferencesData> findPreferences(UUID userId) throws PreferencesLookupException;

  /**
   * Retrieves the language code for a user, falling back to default if not found.
   *
   * @param userId the user ID to look up
   * @return the language code (e.g., "en", "es")
   */
  default String getLanguageCode(final UUID userId) {
    try {
      return findPreferences(userId)
          .map(UserPreferencesData::languageCode)
          .orElse(DEFAULT_LANGUAGE_CODE);
    } catch (final PreferencesLookupException e) {
      return DEFAULT_LANGUAGE_CODE;
    }
  }
}

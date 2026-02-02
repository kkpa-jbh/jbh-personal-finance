package com.jbh.notification.infra.adapters.out.preferences;

import com.jbh.notification.infra.ports.output.UserPreferencesPort;
import com.jbh.preferences.contracts.PreferencesLookupException;
import com.jbh.preferences.contracts.PreferencesLookupPort;
import com.jbh.preferences.contracts.UserPreferencesData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter that delegates to PreferencesLookupPort for direct inter-module communication.
 * This adapter provides a notification-specific facade over the preferences contracts.
 */
@ApplicationScoped
public class DirectPreferencesAdapter implements UserPreferencesPort {

  private static final Logger LOG = LoggerFactory.getLogger(DirectPreferencesAdapter.class);

  private final PreferencesLookupPort preferencesLookupPort;

  @Inject
  public DirectPreferencesAdapter(final PreferencesLookupPort preferencesLookupPort) {
    this.preferencesLookupPort = preferencesLookupPort;
  }

  @Override
  public UserPreferencesData getUserPreferences(final UUID userId)
      throws PreferencesLookupException {
    LOG.debug("Fetching preferences for user: {}", userId);
    return preferencesLookupPort.getPreferences(userId);
  }

  @Override
  public String getUserLanguage(final UUID userId) {
    LOG.debug("Fetching language preference for user: {}", userId);
    return preferencesLookupPort.getLanguageCode(userId);
  }
}

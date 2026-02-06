package com.jbh.preferences.infra.adapters.out.intermodule;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.input.GetTeamPreferencesInputPort;
import com.jbh.preferences.application.core.ports.input.GetUserPreferencesInputPort;
import com.jbh.preferences.contracts.PreferencesLookupException;
import com.jbh.preferences.contracts.PreferencesLookupPort;
import com.jbh.preferences.contracts.TeamPreferencesData;
import com.jbh.preferences.contracts.UserPreferencesData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter that implements PreferencesLookupPort for inter-module communication. Maps internal DTOs
 * to contract DTOs and wraps internal exceptions.
 */
@ApplicationScoped
public class PreferencesLookupAdapter implements PreferencesLookupPort {

  private static final Logger LOG = LoggerFactory.getLogger(PreferencesLookupAdapter.class);

  private final GetUserPreferencesInputPort getUserPreferencesInputPort;
  private final GetTeamPreferencesInputPort getTeamPreferencesInputPort;

  @Inject
  public PreferencesLookupAdapter(
      final GetUserPreferencesInputPort getUserPreferencesInputPort,
      final GetTeamPreferencesInputPort getTeamPreferencesInputPort) {
    this.getUserPreferencesInputPort = getUserPreferencesInputPort;
    this.getTeamPreferencesInputPort = getTeamPreferencesInputPort;
  }

  @Override
  public UserPreferencesData getPreferences(final UUID userId) throws PreferencesLookupException {
    try {
      final UserPreferencesDTO dto = getUserPreferencesInputPort.execute(userId);
      return mapToContractData(dto);
    } catch (final BusinessException e) {
      LOG.warn("Failed to get preferences for user {}: {}", userId, e.getMessage());
      throw new PreferencesLookupException(
          "Failed to retrieve preferences for user: " + userId, userId, e);
    }
  }

  @Override
  public Optional<UserPreferencesData> findPreferences(final UUID userId)
      throws PreferencesLookupException {
    try {
      final UserPreferencesDTO dto = getUserPreferencesInputPort.execute(userId);
      return Optional.of(mapToContractData(dto));
    } catch (final BusinessException e) {
      LOG.debug("Preferences not found for user {}: {}", userId, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public TeamPreferencesData getTeamPreferences(final UUID teamId)
      throws PreferencesLookupException {
    try {
      final TeamPreferencesDTO dto = getTeamPreferencesInputPort.execute(teamId);
      return mapToTeamContractData(dto);
    } catch (final BusinessException e) {
      LOG.warn("Failed to get preferences for team {}: {}", teamId, e.getMessage());
      throw new PreferencesLookupException(
          "Failed to retrieve preferences for team: " + teamId, teamId, e);
    }
  }

  @Override
  public Optional<TeamPreferencesData> findTeamPreferences(final UUID teamId)
      throws PreferencesLookupException {
    try {
      final TeamPreferencesDTO dto = getTeamPreferencesInputPort.execute(teamId);
      return Optional.of(mapToTeamContractData(dto));
    } catch (final BusinessException e) {
      LOG.debug("Preferences not found for team {}: {}", teamId, e.getMessage());
      return Optional.empty();
    }
  }

  private TeamPreferencesData mapToTeamContractData(final TeamPreferencesDTO dto) {
    return new TeamPreferencesData(
        dto.teamId(), dto.defaultCurrency().getCode(), dto.savingsGoal(), dto.lastModifiedBy());
  }

  private UserPreferencesData mapToContractData(final UserPreferencesDTO dto) {
    return new UserPreferencesData(
        dto.userId(),
        dto.defaultLang().code(),
        dto.defaultCurrency().getCode(),
        dto.savingsGoal(),
        dto.defaultProductId());
  }
}

package com.jbh.preferences.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.TeamPreferencesRepository;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import com.jbh.preferences.domain.entity.TeamPreferencesDomain;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamPreferencesServiceImpl implements TeamPreferencesService {

  private static final Logger LOG = LoggerFactory.getLogger(TeamPreferencesServiceImpl.class);
  private static final String TEAM_ID_NULL_MSG = "Team ID cannot be null";

  private final TeamPreferencesRepository preferencesRepository;

  public TeamPreferencesServiceImpl(final TeamPreferencesRepository preferencesRepository) {
    this.preferencesRepository = preferencesRepository;
  }

  @Override
  public TeamPreferencesDTO getPreferences(final UUID teamId) throws BusinessException {
    Objects.requireNonNull(teamId, TEAM_ID_NULL_MSG);

    final Optional<TeamPreferencesDTO> preferences = preferencesRepository.findByTeamId(teamId);

    return preferences.orElseThrow(
        () ->
            new BusinessException(
                PreferencesBusinessExceptionType.TEAM_PREFERENCES_NOT_FOUND, teamId));
  }

  @Override
  public TeamPreferencesDTO createDefaultPreferences(final UUID teamId, final UUID creatorUserId)
      throws BusinessException {
    Objects.requireNonNull(teamId, TEAM_ID_NULL_MSG);

    if (preferencesRepository.existsByTeamId(teamId)) {
      throw new BusinessException(
          PreferencesBusinessExceptionType.TEAM_PREFERENCES_ALREADY_EXIST, teamId);
    }

    final TeamPreferencesDomain defaultPreferences = TeamPreferencesDomain.createDefault(teamId);
    final TeamPreferencesDTO dto = toDTO(defaultPreferences);

    final TeamPreferencesDTO dtoWithCreator =
        TeamPreferencesDTO.internalBuilder()
            .teamId(dto.teamId())
            .defaultCurrency(dto.defaultCurrency())
            .savingsGoal(dto.savingsGoal())
            .metadata(dto.metadata())
            .lastModifiedBy(creatorUserId)
            .createdAt(dto.createdAt())
            .updatedAt(dto.updatedAt())
            .build();

    LOG.debug("Creating default preferences for team: {} by user: {}", teamId, creatorUserId);
    return preferencesRepository.save(dtoWithCreator);
  }

  @Override
  public TeamPreferencesDTO updatePreferences(
      final UUID teamId, final UpdateTeamPreferencesCommand command) throws BusinessException {
    Objects.requireNonNull(teamId, TEAM_ID_NULL_MSG);
    Objects.requireNonNull(command, "Update command cannot be null");

    final TeamPreferencesDTO existingPreferences = getPreferences(teamId);

    final TeamPreferencesDTO updatedDTO =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(command.defaultCurrency())
            .savingsGoal(command.savingsGoal())
            .metadata(command.metadata())
            .lastModifiedBy(command.lastModifiedBy())
            .createdAt(existingPreferences.createdAt())
            .updatedAt(existingPreferences.updatedAt())
            .build();

    LOG.debug(
        "Updating preferences for team: {} by user: {}", teamId, command.lastModifiedBy());
    return preferencesRepository.save(updatedDTO);
  }

  @Override
  public TeamPreferencesDTO getOrCreatePreferences(final UUID teamId, final UUID userId)
      throws BusinessException {
    Objects.requireNonNull(teamId, TEAM_ID_NULL_MSG);

    final Optional<TeamPreferencesDTO> existing = preferencesRepository.findByTeamId(teamId);
    if (existing.isPresent()) {
      return existing.get();
    }

    return createDefaultPreferences(teamId, userId);
  }

  private TeamPreferencesDTO toDTO(final TeamPreferencesDomain domain) {
    return TeamPreferencesDTO.internalBuilder()
        .teamId(domain.getTeamId())
        .defaultCurrency(domain.getDefaultCurrency())
        .savingsGoal(domain.getSavingsGoal())
        .metadata(domain.getMetadata())
        .lastModifiedBy(domain.getLastModifiedBy())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }
}

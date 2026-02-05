package com.jbh.preferences.application.core.services;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.UserPreferencesRepository;
import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import com.jbh.preferences.domain.entity.UserPreferencesDomain;
import com.jbh.preferences.domain.exceptions.PreferencesBusinessExceptionType;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesServiceImpl implements PreferencesService {

  private static final Logger LOG = LoggerFactory.getLogger(PreferencesServiceImpl.class);
  private static final String USER_ID_NULL_MSG = "User ID cannot be null";

  private final UserPreferencesRepository preferencesRepository;

  public PreferencesServiceImpl(final UserPreferencesRepository preferencesRepository) {
    this.preferencesRepository = preferencesRepository;
  }

  @Override
  public UserPreferencesDTO getPreferences(final UUID userId) throws BusinessException {
    Objects.requireNonNull(userId, USER_ID_NULL_MSG);

    final Optional<UserPreferencesDTO> preferences = preferencesRepository.findByUserId(userId);

    if (preferences.isEmpty()) {
      return createDefaultPreferences(userId);
    }

    return preferences.orElseThrow(
        () ->
            new BusinessException(PreferencesBusinessExceptionType.PREFERENCES_NOT_FOUND, userId));
  }

  @Override
  public UserPreferencesDTO createDefaultPreferences(final UUID userId) throws BusinessException {
    Objects.requireNonNull(userId, USER_ID_NULL_MSG);

    if (preferencesRepository.existsByUserId(userId)) {
      throw new BusinessException(
          PreferencesBusinessExceptionType.PREFERENCES_ALREADY_EXIST, userId);
    }

    final UserPreferencesDomain defaultPreferences = UserPreferencesDomain.createDefault(userId);
    final UserPreferencesDTO dto = toDTO(defaultPreferences);

    LOG.debug("Creating default preferences for user: {}", userId);
    return preferencesRepository.save(dto);
  }

  @Override
  public UserPreferencesDTO updatePreferences(
      final UUID userId, final UpdatePreferencesCommand command) throws BusinessException {
    Objects.requireNonNull(userId, USER_ID_NULL_MSG);
    Objects.requireNonNull(command, "Update command cannot be null");

    final UserPreferencesDTO existingPreferences = getPreferences(userId);

    final UserPreferencesDTO updatedDTO =
        UserPreferencesDTO.internalBuilder()
            .userId(userId)
            .defaultLang(command.defaultLang())
            .defaultCurrency(command.defaultCurrency())
            .savingsGoal(command.savingsGoal())
            .defaultAccountId(command.defaultAccountId())
            .metadata(command.metadata())
            .createdAt(existingPreferences.createdAt())
            .updatedAt(existingPreferences.updatedAt())
            .build();

    LOG.debug("Updating preferences for user: {}", userId);
    return preferencesRepository.save(updatedDTO);
  }

  @Override
  public UserPreferencesDTO getOrCreatePreferences(final UUID userId) throws BusinessException {
    Objects.requireNonNull(userId, USER_ID_NULL_MSG);

    final Optional<UserPreferencesDTO> existing = preferencesRepository.findByUserId(userId);
    if (existing.isPresent()) {
      return existing.get();
    }

    return createDefaultPreferences(userId);
  }

  private UserPreferencesDTO toDTO(final UserPreferencesDomain domain) {
    return UserPreferencesDTO.internalBuilder()
        .userId(domain.getUserId())
        .defaultLang(domain.getDefaultLang())
        .defaultCurrency(domain.getDefaultCurrency())
        .savingsGoal(domain.getSavingsGoal())
        .defaultAccountId(domain.getDefaultAccountId())
        .metadata(domain.getMetadata())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }
}

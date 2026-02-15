package com.jbh.preferences.infra.adapters.in.rest.vo;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response for team preferences.
 *
 * <p>This is the public API contract for team preferences data. It maps from internal {@link
 * TeamPreferencesDTO} and is returned by REST controllers.
 */
public record TeamPreferencesResponse(
    UUID teamId,
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    PreferencesMetadata metadata,
    UUID lastModifiedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  /**
   * Creates a response from the internal DTO.
   *
   * @param dto the internal team preferences DTO
   * @return the API response
   */
  public static TeamPreferencesResponse fromDTO(final TeamPreferencesDTO dto) {
    return new TeamPreferencesResponse(
        dto.teamId(),
        dto.defaultCurrency(),
        dto.savingsGoal(),
        dto.metadata(),
        dto.lastModifiedBy(),
        dto.createdAt(),
        dto.updatedAt());
  }
}

package com.jbh.preferences.infra.adapters.in.rest.vo;

import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response for user preferences.
 *
 * <p>This is the public API contract for user preferences data. It maps from internal {@link
 * UserPreferencesDTO} and is returned by REST controllers.
 */
public record UserPreferencesResponse(
    UUID userId,
    Language defaultLang,
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    UUID defaultProductId,
    PreferencesMetadata metadata,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  /**
   * Creates a response from the internal DTO.
   *
   * @param dto the internal user preferences DTO
   * @return the API response
   */
  public static UserPreferencesResponse fromDTO(final UserPreferencesDTO dto) {
    return new UserPreferencesResponse(
        dto.userId(),
        dto.defaultLang(),
        dto.defaultCurrency(),
        dto.savingsGoal(),
        dto.defaultProductId(),
        dto.metadata(),
        dto.createdAt(),
        dto.updatedAt());
  }
}

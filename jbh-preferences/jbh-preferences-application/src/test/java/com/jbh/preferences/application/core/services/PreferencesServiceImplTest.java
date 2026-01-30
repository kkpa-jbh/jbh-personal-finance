package com.jbh.preferences.application.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.UserPreferencesRepository;
import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferencesServiceImplTest {

  @Mock private UserPreferencesRepository preferencesRepository;

  private PreferencesServiceImpl preferencesService;

  @BeforeEach
  void setUp() {
    preferencesService = new PreferencesServiceImpl(preferencesRepository);
  }

  @Test
  void getPreferences_shouldReturnPreferencesWhenFound() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO expectedDto = createSampleDTO(userId);
    when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(expectedDto));

    final UserPreferencesDTO result = preferencesService.getPreferences(userId);

    assertEquals(expectedDto, result);
    verify(preferencesRepository).findByUserId(userId);
  }

  private UserPreferencesDTO createSampleDTO(final UUID userId) {
    return UserPreferencesDTO.internalBuilder()
        .id(PreferencesId.generate())
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void getPreferences_shouldThrowExceptionWhenNotFound() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());

    final var preferences = preferencesService.getPreferences(userId);
  }

  @Test
  void getPreferences_shouldThrowExceptionForNullUserId() {
    assertThrows(NullPointerException.class, () -> preferencesService.getPreferences(null));
  }

  @Test
  void createDefaultPreferences_shouldCreateAndSavePreferences() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    when(preferencesRepository.existsByUserId(userId)).thenReturn(false);
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final UserPreferencesDTO result = preferencesService.createDefaultPreferences(userId);

    assertNotNull(result);
    assertEquals(userId, result.userId());
    assertEquals(Language.DEFAULT, result.defaultLang());
    assertEquals(Currency.COP, result.defaultCurrency());
    verify(preferencesRepository).existsByUserId(userId);
    verify(preferencesRepository).save(any());
  }

  @Test
  void createDefaultPreferences_shouldThrowExceptionWhenPreferencesExist() {
    final UUID userId = UUID.randomUUID();
    when(preferencesRepository.existsByUserId(userId)).thenReturn(true);

    final BusinessException exception =
        assertThrows(
            BusinessException.class, () -> preferencesService.createDefaultPreferences(userId));

    assertTrue(exception.getMessage().contains("already exist"));
    verify(preferencesRepository, never()).save(any());
  }

  @Test
  void createDefaultPreferences_shouldThrowExceptionForNullUserId() {
    assertThrows(
        NullPointerException.class, () -> preferencesService.createDefaultPreferences(null));
  }

  @Test
  void updatePreferences_shouldUpdateAndSavePreferences() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO existingDto = createSampleDTO(userId);
    final UpdatePreferencesCommand command =
        UpdatePreferencesCommand.builder()
            .defaultLang(Language.SPANISH)
            .defaultCurrency(Currency.COP)
            .savingsGoal(new BigDecimal("5000.00"))
            .build();

    when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingDto));
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final UserPreferencesDTO result = preferencesService.updatePreferences(userId, command);

    assertNotNull(result);
    assertEquals(Language.SPANISH, result.defaultLang());
    assertEquals(Currency.COP, result.defaultCurrency());
    assertEquals(new BigDecimal("5000.00"), result.savingsGoal());
    verify(preferencesRepository).save(any());
  }

  @Test
  void updatePreferences_shouldThrowExceptionForNullUserId() {
    final UpdatePreferencesCommand command = UpdatePreferencesCommand.builder().build();
    assertThrows(
        NullPointerException.class, () -> preferencesService.updatePreferences(null, command));
  }

  @Test
  void updatePreferences_shouldThrowExceptionForNullCommand() {
    final UUID userId = UUID.randomUUID();
    assertThrows(
        NullPointerException.class, () -> preferencesService.updatePreferences(userId, null));
  }

  @Test
  void getOrCreatePreferences_shouldReturnExistingPreferences() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    final UserPreferencesDTO existingDto = createSampleDTO(userId);
    when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existingDto));

    final UserPreferencesDTO result = preferencesService.getOrCreatePreferences(userId);

    assertEquals(existingDto, result);
    verify(preferencesRepository, never()).existsByUserId(any());
    verify(preferencesRepository, never()).save(any());
  }

  @Test
  void getOrCreatePreferences_shouldCreateWhenNotFound() throws BusinessException {
    final UUID userId = UUID.randomUUID();
    when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
    when(preferencesRepository.existsByUserId(userId)).thenReturn(false);
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final UserPreferencesDTO result = preferencesService.getOrCreatePreferences(userId);

    assertNotNull(result);
    assertEquals(userId, result.userId());
    verify(preferencesRepository).save(any());
  }
}

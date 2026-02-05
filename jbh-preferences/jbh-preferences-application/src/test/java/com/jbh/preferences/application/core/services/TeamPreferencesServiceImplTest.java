package com.jbh.preferences.application.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.TeamPreferencesRepository;
import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import com.jbh.preferences.domain.vo.Currency;
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
class TeamPreferencesServiceImplTest {

  @Mock private TeamPreferencesRepository preferencesRepository;

  private TeamPreferencesServiceImpl preferencesService;

  @BeforeEach
  void setUp() {
    preferencesService = new TeamPreferencesServiceImpl(preferencesRepository);
  }

  @Test
  void getPreferences_shouldReturnPreferencesWhenFound() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final TeamPreferencesDTO expectedDto = createSampleDTO(teamId);
    when(preferencesRepository.findByTeamId(teamId)).thenReturn(Optional.of(expectedDto));

    final TeamPreferencesDTO result = preferencesService.getPreferences(teamId);

    assertEquals(expectedDto, result);
    verify(preferencesRepository).findByTeamId(teamId);
  }

  private TeamPreferencesDTO createSampleDTO(final UUID teamId) {
    return TeamPreferencesDTO.internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(BigDecimal.ZERO)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void getPreferences_shouldThrowExceptionWhenNotFound() {
    final UUID teamId = UUID.randomUUID();
    when(preferencesRepository.findByTeamId(teamId)).thenReturn(Optional.empty());

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> preferencesService.getPreferences(teamId));

    assertTrue(exception.getMessage().contains("not found"));
    verify(preferencesRepository).findByTeamId(teamId);
  }

  @Test
  void getPreferences_shouldThrowExceptionForNullTeamId() {
    assertThrows(NullPointerException.class, () -> preferencesService.getPreferences(null));
  }

  @Test
  void createDefaultPreferences_shouldCreateAndSavePreferencesWithCreatorUserId()
      throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID creatorUserId = UUID.randomUUID();
    when(preferencesRepository.existsByTeamId(teamId)).thenReturn(false);
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final TeamPreferencesDTO result =
        preferencesService.createDefaultPreferences(teamId, creatorUserId);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(Currency.COP, result.defaultCurrency());
    assertEquals(creatorUserId, result.lastModifiedBy());
    verify(preferencesRepository).existsByTeamId(teamId);
    verify(preferencesRepository).save(any());
  }

  @Test
  void createDefaultPreferences_shouldThrowExceptionWhenPreferencesExist() {
    final UUID teamId = UUID.randomUUID();
    final UUID creatorUserId = UUID.randomUUID();
    when(preferencesRepository.existsByTeamId(teamId)).thenReturn(true);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> preferencesService.createDefaultPreferences(teamId, creatorUserId));

    assertTrue(exception.getMessage().contains("already exist"));
    verify(preferencesRepository, never()).save(any());
  }

  @Test
  void createDefaultPreferences_shouldThrowExceptionForNullTeamId() {
    final UUID creatorUserId = UUID.randomUUID();
    assertThrows(
        NullPointerException.class,
        () -> preferencesService.createDefaultPreferences(null, creatorUserId));
  }

  @Test
  void updatePreferences_shouldUpdateAndSavePreferencesWithLastModifiedBy()
      throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID modifierUserId = UUID.randomUUID();
    final TeamPreferencesDTO existingDto = createSampleDTO(teamId);
    when(preferencesRepository.findByTeamId(teamId)).thenReturn(Optional.of(existingDto));
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final UpdateTeamPreferencesCommand command =
        UpdateTeamPreferencesCommand.builder()
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("5000.00"))
            .lastModifiedBy(modifierUserId)
            .build();

    final TeamPreferencesDTO result = preferencesService.updatePreferences(teamId, command);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(Currency.USD, result.defaultCurrency());
    assertEquals(new BigDecimal("5000.00"), result.savingsGoal());
    assertEquals(modifierUserId, result.lastModifiedBy());
    verify(preferencesRepository).findByTeamId(teamId);
    verify(preferencesRepository).save(any());
  }

  @Test
  void updatePreferences_shouldThrowExceptionForNullTeamId() {
    final UpdateTeamPreferencesCommand command = UpdateTeamPreferencesCommand.builder().build();

    assertThrows(
        NullPointerException.class, () -> preferencesService.updatePreferences(null, command));
  }

  @Test
  void updatePreferences_shouldThrowExceptionForNullCommand() {
    final UUID teamId = UUID.randomUUID();

    assertThrows(
        NullPointerException.class, () -> preferencesService.updatePreferences(teamId, null));
  }

  @Test
  void getOrCreatePreferences_shouldReturnExistingPreferences() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final TeamPreferencesDTO expectedDto = createSampleDTO(teamId);
    when(preferencesRepository.findByTeamId(teamId)).thenReturn(Optional.of(expectedDto));

    final TeamPreferencesDTO result = preferencesService.getOrCreatePreferences(teamId, userId);

    assertEquals(expectedDto, result);
    verify(preferencesRepository).findByTeamId(teamId);
    verify(preferencesRepository, never()).save(any());
  }

  @Test
  void getOrCreatePreferences_shouldCreateWhenNotExists() throws BusinessException {
    final UUID teamId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(preferencesRepository.findByTeamId(teamId)).thenReturn(Optional.empty());
    when(preferencesRepository.existsByTeamId(teamId)).thenReturn(false);
    when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final TeamPreferencesDTO result = preferencesService.getOrCreatePreferences(teamId, userId);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(userId, result.lastModifiedBy());
    verify(preferencesRepository).findByTeamId(teamId);
    verify(preferencesRepository).save(any());
  }
}

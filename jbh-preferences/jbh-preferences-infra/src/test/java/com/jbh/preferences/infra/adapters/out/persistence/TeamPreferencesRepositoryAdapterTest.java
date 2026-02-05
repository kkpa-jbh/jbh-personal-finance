package com.jbh.preferences.infra.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamPreferencesRepositoryAdapterTest {

  @Mock private TeamPreferencesJPARepository jpaRepository;

  @Mock private EntityManager entityManager;

  @InjectMocks private TeamPreferencesRepositoryAdapter adapter;

  private UUID teamId;
  private UUID modifierId;

  @BeforeEach
  void setUp() {
    teamId = UUID.randomUUID();
    modifierId = UUID.randomUUID();
  }

  @Test
  void findByTeamId_shouldReturnDTOWhenFound() {
    final TeamPreferencesJPAEntity entity = createSampleEntity();
    when(jpaRepository.findByTeamId(teamId)).thenReturn(Optional.of(entity));

    final Optional<TeamPreferencesDTO> result = adapter.findByTeamId(teamId);

    assertTrue(result.isPresent());
    assertEquals(teamId, result.get().teamId());
    assertEquals(modifierId, result.get().lastModifiedBy());
    verify(jpaRepository).findByTeamId(teamId);
  }

  private TeamPreferencesJPAEntity createSampleEntity() {
    final TeamPreferencesJPAEntity entity = new TeamPreferencesJPAEntity();
    entity.setTeamId(teamId);
    entity.setDefaultCurrency(Currency.COP);
    entity.setSavingsGoal(BigDecimal.ZERO);
    entity.setLastModifiedBy(modifierId);
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());
    return entity;
  }

  @Test
  void findByTeamId_shouldReturnEmptyWhenNotFound() {
    when(jpaRepository.findByTeamId(teamId)).thenReturn(Optional.empty());

    final Optional<TeamPreferencesDTO> result = adapter.findByTeamId(teamId);

    assertFalse(result.isPresent());
    verify(jpaRepository).findByTeamId(teamId);
  }

  @Test
  void findByTeamId_shouldThrowExceptionForNullTeamId() {
    assertThrows(IllegalArgumentException.class, () -> adapter.findByTeamId(null));
  }

  @Test
  void save_shouldPersistNewEntity() {
    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.COP)
            .savingsGoal(BigDecimal.ZERO)
            .metadata(PreferencesMetadata.empty())
            .lastModifiedBy(modifierId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    when(jpaRepository.existsByTeamId(teamId)).thenReturn(false);
    doNothing().when(jpaRepository).persist(any(TeamPreferencesJPAEntity.class));

    final TeamPreferencesDTO result = adapter.save(dto);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(modifierId, result.lastModifiedBy());
    verify(jpaRepository).persist(any(TeamPreferencesJPAEntity.class));
    verify(jpaRepository, never()).getEntityManager();
  }

  @Test
  void save_shouldMergeExistingEntity() {
    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("5000.00"))
            .metadata(PreferencesMetadata.empty())
            .lastModifiedBy(modifierId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    final TeamPreferencesJPAEntity mergedEntity = createSampleEntity();
    mergedEntity.setDefaultCurrency(Currency.USD);
    mergedEntity.setSavingsGoal(new BigDecimal("5000.00"));

    when(jpaRepository.existsByTeamId(teamId)).thenReturn(true);
    when(jpaRepository.getEntityManager()).thenReturn(entityManager);
    when(entityManager.merge(any(TeamPreferencesJPAEntity.class))).thenReturn(mergedEntity);

    final TeamPreferencesDTO result = adapter.save(dto);

    assertNotNull(result);
    assertEquals(teamId, result.teamId());
    assertEquals(Currency.USD, result.defaultCurrency());
    assertEquals(modifierId, result.lastModifiedBy());
    verify(jpaRepository).getEntityManager();
    verify(entityManager).merge(any(TeamPreferencesJPAEntity.class));
    verify(jpaRepository, never()).persist(any(TeamPreferencesJPAEntity.class));
  }

  @Test
  void save_shouldThrowExceptionForNullDTO() {
    assertThrows(IllegalArgumentException.class, () -> adapter.save(null));
  }

  @Test
  void existsByTeamId_shouldReturnTrueWhenExists() {
    when(jpaRepository.existsByTeamId(teamId)).thenReturn(true);

    final boolean result = adapter.existsByTeamId(teamId);

    assertTrue(result);
    verify(jpaRepository).existsByTeamId(teamId);
  }

  @Test
  void existsByTeamId_shouldReturnFalseWhenNotExists() {
    when(jpaRepository.existsByTeamId(teamId)).thenReturn(false);

    final boolean result = adapter.existsByTeamId(teamId);

    assertFalse(result);
    verify(jpaRepository).existsByTeamId(teamId);
  }

  @Test
  void existsByTeamId_shouldThrowExceptionForNullTeamId() {
    assertThrows(IllegalArgumentException.class, () -> adapter.existsByTeamId(null));
  }

  @Test
  void save_shouldPreserveLastModifiedByOnCreate() {
    final UUID creatorUserId = UUID.randomUUID();
    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.defaultBuilder(teamId)
            .lastModifiedBy(creatorUserId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    when(jpaRepository.existsByTeamId(teamId)).thenReturn(false);
    doNothing().when(jpaRepository).persist(any(TeamPreferencesJPAEntity.class));

    final TeamPreferencesDTO result = adapter.save(dto);

    assertEquals(creatorUserId, result.lastModifiedBy());
    verify(jpaRepository).persist(any(TeamPreferencesJPAEntity.class));
  }

  @Test
  void save_shouldPreserveLastModifiedByOnUpdate() {
    final UUID updaterUserId = UUID.randomUUID();
    final TeamPreferencesDTO dto =
        TeamPreferencesDTO.internalBuilder()
            .teamId(teamId)
            .defaultCurrency(Currency.USD)
            .savingsGoal(new BigDecimal("7500.00"))
            .metadata(PreferencesMetadata.empty())
            .lastModifiedBy(updaterUserId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    final TeamPreferencesJPAEntity mergedEntity = createSampleEntity();
    mergedEntity.setLastModifiedBy(updaterUserId);

    when(jpaRepository.existsByTeamId(teamId)).thenReturn(true);
    when(jpaRepository.getEntityManager()).thenReturn(entityManager);
    when(entityManager.merge(any(TeamPreferencesJPAEntity.class))).thenReturn(mergedEntity);

    final TeamPreferencesDTO result = adapter.save(dto);

    assertEquals(updaterUserId, result.lastModifiedBy());
    verify(entityManager).merge(any(TeamPreferencesJPAEntity.class));
  }
}

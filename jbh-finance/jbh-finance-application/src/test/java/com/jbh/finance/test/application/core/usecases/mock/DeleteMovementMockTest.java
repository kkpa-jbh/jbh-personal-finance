package com.jbh.finance.test.application.core.usecases.mock;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.input.DeleteMovementInputPort;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleServiceImpl;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.application.core.ports.output.movement.InMemoryMovementQueryRepository;
import com.jbh.finance.test.application.core.ports.output.movement.InMemoryMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteMovementMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();
  private final InMemoryMovementQueryRepository movementQueryRepository =
      new InMemoryMovementQueryRepository();
  private DeleteMovementUseCase useCase;
  @Mock private ProductRepository productRepository;
  @Mock private ProcessMovementService processMovementService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    movementQueryRepository.clearStorage();

    final ProductLifecycleService productLifecycleService =
        new ProductLifecycleServiceImpl(productRepository);
    final MovementLifecycleService movementLifecycleService =
        new MovementLifecycleServiceImpl(
            new InMemoryMovementRepository(movementQueryRepository), movementQueryRepository);

    useCase =
        new DeleteMovementInputPort(
            movementLifecycleService, productLifecycleService, processMovementService);
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    assertThrows(
        GenericSpecificationException.class,
        () -> useCase.deleteMovement(null, TEST_PRODUCT_ID, UUID.randomUUID()));
  }

  @Test
  void shouldThrowWhenProductNotFound() throws BusinessException {
    when(productRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.empty());

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> useCase.deleteMovement(TEST_USER_ID, TEST_PRODUCT_ID, UUID.randomUUID()));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(processMovementService, never()).reverseMovementProcessingBalances(any(), any());
  }

  @Test
  void shouldThrowWhenMovementNotFound() throws BusinessException {
    when(productRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(ProductMapper.toDTO(createActiveProduct())));

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> useCase.deleteMovement(TEST_USER_ID, TEST_PRODUCT_ID, UUID.randomUUID()));

    assertEquals(
        BusinessApplicationExceptionType.MOVEMENT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(processMovementService, never()).reverseMovementProcessingBalances(any(), any());
  }

  private ProductDomain createActiveProduct() {
    return new ProductDomain(
        TEST_PRODUCT_ID,
        "Test Product",
        ProductType.SAVINGS,
        TEST_USER_ID,
        JBH_ZERO,
        JBH_ZERO,
        JBH_ZERO,
        true,
        LocalDateTime.now(),
        LocalDateTime.now(),
        JBH_ZERO,
        ProductMetadata.empty());
  }

  @Test
  void shouldThrowWhenMovementBelongsToDifferentProduct() throws BusinessException {
    when(productRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(ProductMapper.toDTO(createActiveProduct())));

    final MovementDTO movement = buildRemovableMovement(ProductId.generate());
    movementQueryRepository.save(movement);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> useCase.deleteMovement(TEST_USER_ID, TEST_PRODUCT_ID, movement.id().value()));

    assertEquals(
        BusinessApplicationExceptionType.MOVEMENT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(processMovementService, never()).reverseMovementProcessingBalances(any(), any());
  }

  private MovementDTO buildRemovableMovement(final ProductId productId) {
    return MovementDTO.builder()
        .id(MovementId.generate())
        .productId(productId)
        .movementType(MovementType.DEPOSIT)
        .movementAmount(new BigDecimal("100.00"))
        .movementDate(LocalDate.now())
        .balanceSnapshot(new BigDecimal("1000.00"))
        .createdAt(LocalDateTime.now())
        .description("Test movement")
        .build();
  }

  @Test
  void shouldThrowWhenMovementCannotBeRemoved() throws BusinessException {
    when(productRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(ProductMapper.toDTO(createActiveProduct())));

    final MovementDTO movement = buildNonRemovableMovement(TEST_PRODUCT_ID);
    movementQueryRepository.save(movement);

    final BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> useCase.deleteMovement(TEST_USER_ID, TEST_PRODUCT_ID, movement.id().value()));

    assertEquals(
        BusinessApplicationExceptionType.MOVEMENT_CANNOT_BE_REMOVED.getMessage(),
        exception.getMessage());
    verify(processMovementService, never()).reverseMovementProcessingBalances(any(), any());
  }

  private MovementDTO buildNonRemovableMovement(final ProductId productId) {
    return MovementDTO.builder()
        .id(MovementId.generate())
        .productId(productId)
        .movementType(MovementType.DEPOSIT)
        .movementAmount(new BigDecimal("100.00"))
        .movementDate(LocalDate.now())
        .balanceSnapshot(new BigDecimal("1000.00"))
        .createdAt(LocalDateTime.now().minusMonths(2))
        .description("Old movement - cannot be removed")
        .build();
  }

  @Test
  void shouldDeleteMovementSuccessfully() throws BusinessException {
    when(productRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(ProductMapper.toDTO(createActiveProduct())));

    final MovementDTO movement = buildRemovableMovement(TEST_PRODUCT_ID);
    movementQueryRepository.save(movement);

    assertDoesNotThrow(
        () -> useCase.deleteMovement(TEST_USER_ID, TEST_PRODUCT_ID, movement.id().value()));

    verify(processMovementService).reverseMovementProcessingBalances(any(), any());
  }
}

package com.jbh.finance.test.application.feature.product.usecases;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.product.commands.UpdateProductStatusCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.ports.input.UpdateProductStatusInputPort;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
import com.jbh.finance.application.feature.product.usecases.UpdateProductStatusUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UpdateProductStatusTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();
  private static final String PRODUCT_NAME = "Test Product";

  private UpdateProductStatusUseCase useCase;
  private ProductLifecycleService accountService;
  @Mock private ProductRepository accountRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    accountService = new ProductLifecycleServiceImpl(accountRepository);
    useCase = new UpdateProductStatusInputPort(accountService);
  }

  @Test
  void shouldDeactivateActiveProduct() throws BusinessException {
    final ProductDomain productDomain = createActiveProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));
    when(accountRepository.save(any(ProductDTO.class))).thenAnswer(inv -> inv.getArgument(0));

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(TEST_USER_ID, TEST_PRODUCT_ID, false);

    final ProductDTO result = useCase.execute(command);

    final ArgumentCaptor<ProductDTO> captor = ArgumentCaptor.forClass(ProductDTO.class);
    verify(accountRepository).save(captor.capture());

    final ProductDTO savedProduct = captor.getValue();
    assertFalse(savedProduct.isActive());
    assertFalse(result.isActive());
  }

  private ProductDomain createActiveProduct() {
    return new ProductDomain(
        TEST_PRODUCT_ID,
        PRODUCT_NAME,
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
  void shouldActivateInactiveProduct() throws BusinessException {
    final ProductDomain productDomain = createInactiveProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));
    when(accountRepository.save(any(ProductDTO.class))).thenAnswer(inv -> inv.getArgument(0));

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(TEST_USER_ID, TEST_PRODUCT_ID, true);

    final ProductDTO result = useCase.execute(command);

    final ArgumentCaptor<ProductDTO> captor = ArgumentCaptor.forClass(ProductDTO.class);
    verify(accountRepository).save(captor.capture());

    final ProductDTO savedProduct = captor.getValue();
    assertTrue(savedProduct.isActive());
    assertTrue(result.isActive());
  }

  private ProductDomain createInactiveProduct() {
    return new ProductDomain(
        TEST_PRODUCT_ID,
        PRODUCT_NAME,
        ProductType.SAVINGS,
        TEST_USER_ID,
        JBH_ZERO,
        JBH_ZERO,
        JBH_ZERO,
        false,
        LocalDateTime.now(),
        LocalDateTime.now(),
        JBH_ZERO,
        ProductMetadata.empty());
  }

  @Test
  void shouldNotSaveWhenStatusAlreadyMatches() throws BusinessException {
    final ProductDomain productDomain = createActiveProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(TEST_USER_ID, TEST_PRODUCT_ID, true);

    final ProductDTO result = useCase.execute(command);

    verify(accountRepository, never()).save(any(ProductDTO.class));
    assertTrue(result.isActive());
  }

  @Test
  void shouldThrowWhenProductNotFound() {
    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.empty());

    final UpdateProductStatusCommand command =
        new UpdateProductStatusCommand(TEST_USER_ID, TEST_PRODUCT_ID, false);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> useCase.execute(command));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(accountRepository, never()).save(any(ProductDTO.class));
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    assertThrows(
        GenericSpecificationException.class,
        () -> new UpdateProductStatusCommand(null, TEST_PRODUCT_ID, true));
  }

  @Test
  void shouldThrowWhenProductIdIsNull() {
    assertThrows(
        GenericSpecificationException.class,
        () -> new UpdateProductStatusCommand(TEST_USER_ID, null, true));
  }

  @Test
  void shouldThrowWhenCommandIsNull() {
    assertThrows(GenericSpecificationException.class, () -> useCase.execute(null));
  }
}

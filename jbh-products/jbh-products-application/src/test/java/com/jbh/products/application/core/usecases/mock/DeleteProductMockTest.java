package com.jbh.products.application.core.usecases.mock;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.application.core.ports.input.DeleteProductInputPort;
import com.jbh.products.application.core.ports.output.ProductRepository;
import com.jbh.products.application.core.services.account.ProductServiceImpl;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.DeleteProductUseCase;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;
import com.jbh.products.domain.product.ProductDomain;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductMetadata;
import com.jbh.products.domain.product.vo.ProductType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteProductMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();
  private static final String PRODUCT_NAME = "Test Product";

  private DeleteProductUseCase useCase;
  private ProductsService accountService;
  @Mock private ProductRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    accountService = new ProductServiceImpl(accountRepository);
    useCase = new DeleteProductInputPort(accountService);
  }

  @Test
  public void shouldDeleteProduct() throws BusinessException {
    final ProductDomain productDomain = createActiveProduct();
    final ProductDTO productDTO = AccountMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));
    when(accountRepository.save(any(ProductDTO.class))).thenAnswer(inv -> inv.getArgument(0));

    final DeleteProductCommand command = new DeleteProductCommand(TEST_USER_ID, TEST_PRODUCT_ID);

    useCase.execute(command);

    final ArgumentCaptor<ProductDTO> captor = ArgumentCaptor.forClass(ProductDTO.class);
    verify(accountRepository).deleteById(TEST_PRODUCT_ID);
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
  public void shouldThrowWhenProductNotFound() {
    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.empty());

    final DeleteProductCommand command = new DeleteProductCommand(TEST_USER_ID, TEST_PRODUCT_ID);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> useCase.execute(command));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(accountRepository, never()).save(any(ProductDTO.class));
  }

  @Test
  public void shouldThrowWhenProductAlreadyDeleted() {
    final ProductDomain inactiveProduct = createInactiveProduct();
    final ProductDTO productDTO = AccountMapper.toDTO(inactiveProduct);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.empty());

    final DeleteProductCommand command = new DeleteProductCommand(TEST_USER_ID, TEST_PRODUCT_ID);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> useCase.execute(command));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(accountRepository, never()).deleteById(TEST_PRODUCT_ID);
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
  public void shouldThrowWhenUserIdIsNull() {
    assertThrows(
        GenericSpecificationException.class, () -> new DeleteProductCommand(null, TEST_PRODUCT_ID));
  }

  @Test
  public void shouldThrowWhenProductIdIsNull() {
    assertThrows(
        GenericSpecificationException.class, () -> new DeleteProductCommand(TEST_USER_ID, null));
  }

  @Test
  public void shouldThrowWhenCommandIsNull() {
    assertThrows(GenericSpecificationException.class, () -> useCase.execute(null));
  }
}

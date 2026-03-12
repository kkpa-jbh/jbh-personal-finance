package com.jbh.finance.test.application.core.usecases.mock;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.product.commands.EditProductCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.ports.input.EditProductInputPort;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleServiceImpl;
import com.jbh.finance.application.feature.product.usecases.EditProductUseCase;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EditProductMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private static final ProductId TEST_PRODUCT_ID = ProductId.generate();
  private static final String ORIGINAL_NAME = "Original Product";
  private static final String UPDATED_NAME = "Updated Product";

  private EditProductUseCase useCase;
  private ProductLifecycleService accountService;
  @Mock private ProductRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    accountService = new ProductLifecycleServiceImpl(accountRepository);
    useCase = new EditProductInputPort(accountService);
  }

  @Test
  public void shouldEditProductNameOnly() throws BusinessException {
    final ProductDomain productDomain = createSavingProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));
    when(accountRepository.save(any(ProductDTO.class))).thenAnswer(inv -> inv.getArgument(0));

    final EditProductCommand command =
        new EditProductCommand(TEST_USER_ID, TEST_PRODUCT_ID, UPDATED_NAME, null);

    final ProductDTO result = useCase.execute(command);

    assertNotNull(result);
    assertEquals(UPDATED_NAME, result.name());
    verify(accountRepository).save(any(ProductDTO.class));
  }

  private ProductDomain createSavingProduct() {
    return new ProductDomain(
        TEST_PRODUCT_ID,
        ORIGINAL_NAME,
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
  public void shouldEditProductNameAndMetadata() throws BusinessException {
    final ProductDomain productDomain = createSavingProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(productDomain);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));
    when(accountRepository.save(any(ProductDTO.class))).thenAnswer(inv -> inv.getArgument(0));

    final ProductMetadata newMetadata = ProductMetadata.empty();
    final EditProductCommand command =
        new EditProductCommand(TEST_USER_ID, TEST_PRODUCT_ID, UPDATED_NAME, newMetadata);

    final ProductDTO result = useCase.execute(command);

    assertNotNull(result);
    assertEquals(UPDATED_NAME, result.name());
    verify(accountRepository).save(any(ProductDTO.class));
  }

  @Test
  public void shouldThrowWhenProductNotFound() {
    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.empty());

    final EditProductCommand command =
        new EditProductCommand(TEST_USER_ID, TEST_PRODUCT_ID, UPDATED_NAME, null);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> useCase.execute(command));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_FOUND.getMessage(), exception.getMessage());
    verify(accountRepository, never()).save(any(ProductDTO.class));
  }

  @Test
  public void shouldThrowWhenProductIsInactive() {
    final ProductDomain inactiveProduct = createInactiveProduct();
    final ProductDTO productDTO = ProductMapper.toDTO(inactiveProduct);

    when(accountRepository.findByUserAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
        .thenReturn(Optional.of(productDTO));

    final EditProductCommand command =
        new EditProductCommand(TEST_USER_ID, TEST_PRODUCT_ID, UPDATED_NAME, null);

    final BusinessException exception =
        assertThrows(BusinessException.class, () -> useCase.execute(command));

    assertEquals(
        BusinessApplicationExceptionType.PRODUCT_NOT_ACTIVE.getMessage(), exception.getMessage());
    verify(accountRepository, never()).save(any(ProductDTO.class));
  }

  private ProductDomain createInactiveProduct() {
    return new ProductDomain(
        TEST_PRODUCT_ID,
        ORIGINAL_NAME,
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
  public void shouldThrowWhenNeitherNameNorMetadataProvided() {
    assertThrows(
        GenericSpecificationException.class,
        () -> new EditProductCommand(TEST_USER_ID, TEST_PRODUCT_ID, null, null));
  }

  @Test
  public void shouldThrowWhenCommandIsNull() {
    assertThrows(GenericSpecificationException.class, () -> useCase.execute(null));
  }
}

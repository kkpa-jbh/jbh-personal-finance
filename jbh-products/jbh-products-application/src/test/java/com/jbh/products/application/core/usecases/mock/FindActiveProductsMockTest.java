package com.jbh.products.application.core.usecases.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.ports.input.FindActiveProductsInputPort;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.FindProductsUseCase;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductType;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FindActiveProductsMockTest {

  private static final UUID TEST_USER_ID = UUID.randomUUID();
  private FindProductsUseCase useCase;
  @Mock private ProductsService accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    useCase = new FindActiveProductsInputPort(accountRepository);
  }

  @Test
  public void shouldReturnActiveProductsForUser() {
    final ProductDTO product1 =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, ProductId.generate(), "Savings Account", ProductType.SAVINGS)
            .isActive(true)
            .build();
    final ProductDTO product2 =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, ProductId.generate(), "Investment Account", ProductType.INVESTMENT)
            .isActive(true)
            .build();

    final List<ProductDTO> expectedProducts = List.of(product1, product2);

    when(accountRepository.findActiveByUserId(TEST_USER_ID)).thenReturn(expectedProducts);

    final List<ProductDTO> result = useCase.findActiveByUserId(TEST_USER_ID);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("Savings Account", result.get(0).name());
    assertEquals("Investment Account", result.get(1).name());
    assertTrue(result.stream().allMatch(ProductDTO::isActive));

    verify(accountRepository).findActiveByUserId(TEST_USER_ID);
  }

  @Test
  public void shouldReturnEmptyListWhenNoActiveProducts() {
    when(accountRepository.findActiveByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

    final List<ProductDTO> result = useCase.findActiveByUserId(TEST_USER_ID);

    assertNotNull(result);
    assertTrue(result.isEmpty());

    verify(accountRepository).findActiveByUserId(TEST_USER_ID);
  }

  @Test
  public void shouldThrowExceptionWhenUserIdIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.findActiveByUserId(null));
  }

  @Test
  public void shouldReturnOnlyActiveProducts() {
    final ProductDTO activeProduct =
        ProductDTO.defaultBuilder(
                TEST_USER_ID, ProductId.generate(), "Active Account", ProductType.SAVINGS)
            .isActive(true)
            .build();

    when(accountRepository.findActiveByUserId(TEST_USER_ID)).thenReturn(List.of(activeProduct));

    final List<ProductDTO> result = useCase.findActiveByUserId(TEST_USER_ID);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).isActive());

    verify(accountRepository).findActiveByUserId(TEST_USER_ID);
  }
}

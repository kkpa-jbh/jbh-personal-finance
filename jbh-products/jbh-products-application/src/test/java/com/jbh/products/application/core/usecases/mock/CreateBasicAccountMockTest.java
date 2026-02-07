package com.jbh.products.application.core.usecases.mock;

import static com.jbh.products.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.ports.input.CreateProductInputPort;
import com.jbh.products.application.core.ports.output.ProductRepository;
import com.jbh.products.application.core.services.account.ProductServiceImpl;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.vo.commands.CreateProductCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CreateBasicAccountMockTest {

  final UUID userId = UUID.randomUUID();
  final ProductType type = ProductType.SAVINGS;
  final String testAccountName = "Test Account";
  private CreateProductUseCase useCase;
  private ProductsService accountService;
  @Mock private ProductRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    accountService = new ProductServiceImpl(accountRepository);

    useCase = new CreateProductInputPort(accountService);
  }

  @Test
  public void shouldCreateAccount() throws BusinessException {

    final ProductDTO mockedAccount =
        ProductDTO.defaultBuilder(userId, ProductId.generate(), testAccountName, type).build();

    when(accountRepository.save(any())).thenReturn(mockedAccount);

    final CreateProductCommand command = createBasicAccountCommand(userId, testAccountName, type);
    final ProductDTO accountDTO = useCase.execute(command);

    // Verify output
    assertNotNull(accountDTO);
    assertNotNull(accountDTO.id());
    assertEquals(testAccountName, accountDTO.name());
    assertEquals(type, accountDTO.type());
    assertEquals(userId, accountDTO.userId());

    // Capture the argument passed to accountService.save()
    final ArgumentCaptor<ProductDTO> captor = ArgumentCaptor.forClass(ProductDTO.class);
    verify(accountRepository).save(captor.capture());

    final ProductDTO captured = captor.getValue();

    // Verify that the AccountDomain was mapped correctly
    assertEquals(testAccountName, captured.name());
    assertEquals(type, captured.type());
    assertEquals(userId, captured.userId());
    assertNotNull(captured.currentBalance());
  }

  @Test
  public void shouldThrowExceptionWhenInvalidCommand() {
    assertThrows(GenericSpecificationException.class, () -> useCase.execute(null));
  }
}

package com.jbh.account.application.core.usecases.mock;

import static com.jbh.account.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.ports.input.CreateAccountInputPort;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.account.AccountServiceImpl;
import com.jbh.account.application.core.usecases.CreateProductUseCase;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ProductType;
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
  private AccountService accountService;
  @Mock private AccountRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    accountService = new AccountServiceImpl(accountRepository);

    useCase = new CreateAccountInputPort(accountService);
  }

  @Test
  public void shouldCreateAccount() throws AccountBusinessException {

    final ProductDTO mockedAccount =
        ProductDTO.defaultBuilder(userId, AccountId.generate(), testAccountName, type).build();

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

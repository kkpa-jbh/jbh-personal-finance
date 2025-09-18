package com.jbh.account.application.accounts.usecases.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.ports.input.CreateAccountInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.services.AccountServiceImpl;
import com.jbh.account.application.accounts.usecases.CreateAccountUseCase;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CreateBasicAccountMockTest {

  private CreateAccountUseCase useCase;

  private AccountService accountService;

  @Mock private AccountRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    accountService = new AccountServiceImpl(accountRepository);

    useCase = new CreateAccountInputPort(accountService);
  }

  @Test
  public void shouldCreateAccount() {
    final UUID userId = UUID.randomUUID();
    final AccountType type = AccountType.SAVINGS;
    final String testAccountName = "Test Account";

    final AccountDTO mockedAccount =
        AccountDTO.builder()
            .id(AccountId.generate())
            .name(testAccountName)
            .type(type)
            .userId(userId)
            .build();

    when(accountRepository.save(any())).thenReturn(mockedAccount);

    final CreateBasicAccountCommand command =
        new CreateBasicAccountCommand(userId, testAccountName, type);
    final AccountDTO accountDTO = useCase.execute(command);

    // Verify output
    assertNotNull(accountDTO);
    assertNotNull(accountDTO.id());
    assertEquals(testAccountName, accountDTO.name());
    assertEquals(type, accountDTO.type());
    assertEquals(userId, accountDTO.userId());

    // Capture the argument passed to accountService.save()
    final ArgumentCaptor<AccountDTO> captor = ArgumentCaptor.forClass(AccountDTO.class);
    verify(accountRepository).save(captor.capture());

    final AccountDTO captured = captor.getValue();

    // Verify that the AccountDomain was mapped correctly
    assertEquals(testAccountName, captured.name());
    assertEquals(type, captured.type());
    assertEquals(userId, captured.userId());
    assertNotNull(captured.currentBalance());
  }
}

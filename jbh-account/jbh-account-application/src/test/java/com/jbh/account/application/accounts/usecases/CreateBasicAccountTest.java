package com.jbh.account.application.accounts.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.account.application.accounts.ports.input.CreateAccountInputPort;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.AccountService;
import com.jbh.account.application.accounts.services.AccountServiceImpl;
import com.jbh.account.application.accounts.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CreateBasicAccountTest {

  private CreateAccountUseCase useCase;

  private AccountService accountService;

  @Mock
  private AccountRepository accountRepository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    accountService = new AccountServiceImpl(accountRepository);

    useCase = new CreateAccountInputPort(accountService);
  }


  @Test
  public void shouldCreateAccount() {
    UUID userId = UUID.randomUUID();
    AccountType type = AccountType.SAVINGS;
    String testAccountName = "Test Account";

    AccountDomainDTO mockedAccount = AccountDomainDTO.builder()
        .id(AccountId.generate())
        .name(testAccountName)
        .type(type)
        .userId(userId)
        .build();

    when(accountRepository.save(any())).thenReturn(mockedAccount);

    CreateBasicAccountCommand command = new CreateBasicAccountCommand(userId, testAccountName, type);
    AccountDomainDTO accountDTO = useCase.execute(command);

    // Verify output
    assertNotNull(accountDTO);
    assertNotNull(accountDTO.getId());
    assertEquals(testAccountName, accountDTO.getName());
    assertEquals(type, accountDTO.getType());
    assertEquals(userId, accountDTO.getUserId());

    // Capture the argument passed to accountService.save()
    ArgumentCaptor<AccountDomainDTO> captor = ArgumentCaptor.forClass(AccountDomainDTO.class);
    verify(accountRepository).save(captor.capture());

    AccountDomainDTO captured = captor.getValue();

    // Verify that the AccountDomain was mapped correctly
    assertEquals(testAccountName, captured.getName());
    assertEquals(type, captured.getType());
    assertEquals(userId, captured.getUserId());
    assertNotNull(captured.getCurrentBalance());
  }


}

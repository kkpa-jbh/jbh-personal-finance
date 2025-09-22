package com.jbh.account.application.core.services.account;

import static com.jbh.account.application.core.mappers.AccountMapper.toDTO;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import java.util.Optional;
import java.util.UUID;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;

  public AccountServiceImpl(final AccountRepository accountRepo) {
    this.accountRepo = accountRepo;
  }

  @Override
  public Optional<AccountDTO> findByUserAndAccountId(final UUID userId, final AccountId accountId) {
    return accountRepo.findByUserAndAccountId(userId, accountId);
  }

  @Override
  public Optional<AccountDTO> findByAccountId(final AccountId accountId) {
    return accountRepo.findByAccountId(accountId);
  }

  @Override
  public AccountDTO save(final AccountDomain account) {
    return accountRepo.save(toDTO(account));
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return accountRepo.save(account);
  }

  @Override
  public AccountDTO syncByMonthlyReport(final AccountMonthlyBalanceDTO monthlyBalance) {
    final AccountId accountId = monthlyBalance.accountId();
    final Optional<AccountDTO> accountDTO = findByAccountId(accountId);

    if (accountDTO.isEmpty()) {
      throw new IllegalArgumentException("Account not found");
    }

    final AccountDomain accountDomain = AccountMapper.toDomain(accountDTO.get());
    accountDomain.syncByMonthlyReport(monthlyBalance.closingBalance());
    return save(accountDomain);
  }
}

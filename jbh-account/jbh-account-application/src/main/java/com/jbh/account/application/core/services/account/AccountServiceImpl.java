package com.jbh.account.application.core.services.account;

import static com.jbh.account.application.core.mappers.AccountMapper.toDTO;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;

  private final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

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
  public AccountDTO save(final AccountDTO account) {
    return accountRepo.save(account);
  }

  @Override
  public AccountDTO save(final AccountDomain account) {
    return accountRepo.save(toDTO(account));
  }

  @Override
  public AccountDTO syncByMonthlyReport(final MonthlyBalanceDTO monthlyBalance) {
    final AccountId accountId = monthlyBalance.accountId();
    final Optional<AccountDTO> accountDTO = findByAccountId(accountId);

    if (accountDTO.isEmpty()) {
      throw new IllegalArgumentException("Account not found");
    }

    final AccountDomain accountDomain = AccountMapper.toDomain(accountDTO.get());
    accountDomain.syncByMonthlyReport(monthlyBalance.closingBalance());
    return save(accountDomain);
  }

  @Override
  public AccountDTO syncByMovement(
      final AccountPK accountPK,
      final MovementDTO movement,
      final boolean isMonthOfficiallyReported) {

    final AccountDomain accountDomain = findOrThrow(accountPK);
    accountDomain.syncBalancesByMovement(
        MovementMapper.toDomain(movement), isMonthOfficiallyReported);

    return toDTO(accountDomain);
  }

  private AccountDomain findOrThrow(final AccountPK accountPK) {
    final UUID userId = accountPK.userId();
    final AccountId accountId = accountPK.accountId();

    if (userId == null) {
      log.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    final AccountDTO accountDTO =
        findByUserAndAccountId(userId, accountId)
            .orElseThrow(
                () -> {
                  log.error(
                      "Account not found for user: {} and account: {}", userId, accountId.value());
                  return new IllegalArgumentException("Account not found");
                });

    return AccountMapper.toDomain(accountDTO);
  }
}

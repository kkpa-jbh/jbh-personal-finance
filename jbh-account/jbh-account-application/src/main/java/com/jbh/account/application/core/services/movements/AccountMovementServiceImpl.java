package com.jbh.account.application.core.services.movements;

import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountMovementServiceImpl implements AccountMovementService {
  private static final Logger log = LoggerFactory.getLogger(AccountMovementServiceImpl.class);
  private final AccountMovementRepository movementRepo;

  public AccountMovementServiceImpl(final AccountMovementRepository movementRepo) {
    this.movementRepo = movementRepo;
  }

  @Override
  public void addDividendsMovement(final MonthlyBalanceDTO monthlyBalanceDTO) {
    if (monthlyBalanceDTO == null) {
      log.warn("No monthly balance to add dividends movement");
      return;
    }

    final var accountId = monthlyBalanceDTO.accountId();
    final var period = monthlyBalanceDTO.period();
    final var closingBalance = withJBHDecimals(monthlyBalanceDTO.closingBalance());
    final var monthlyProfitReported = withJBHDecimals(monthlyBalanceDTO.monthlyProfitReported());

    if (monthlyProfitReported != null) {
      log.info(
          "Adding {} dividends movement for account {} and period {}",
          monthlyProfitReported,
          accountId,
          period);

      final AccountMovementDomain dividendsMovement =
          AccountMovementDomain.with(
              accountId,
              period.plusMonths(1).atDay(1),
              monthlyProfitReported,
              closingBalance,
              MovementType.DEPOSIT,
              MovementCategoryDomain.withCategoryType(IncomeCategory.DIVIDENDS));

      movementRepo.save(MovementMapper.toDTO(dividendsMovement));

      log.info(
          "Dividends movement added successfully for account {} and period {}", accountId, period);
    }
  }
}

package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Test builder for creating AccountDomain instances with basic movement data for existing accounts.
 * This builder is intended for test purposes only and should not be used in production code.
 */
public class AccountDomainTestBuilder {

  private AccountId accountId;
  private UUID userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;

  private AccountDomainTestBuilder() {}

  /**
   * Convenience method to maintain backward compatibility with the old static factory method.
   * Creates an AccountDomain with basic movement data for an existing account.
   *
   * @param accountId Account identifier
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return AccountDomain instance
   */
  public static AccountDomain withBasicMovementForExisting(
      final AccountId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    return builder()
        .withAccountId(accountId)
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .build();
  }

  /**
   * Builds an AccountDomain instance with basic movement data for an existing account. This method
   * uses reflection to set private fields since it's for testing purposes.
   *
   * @return AccountDomain instance configured with the builder's data
   */
  public AccountDomain build() {

    return new AccountDomain(
        accountId,
        "DEFAULT_ACCOUNT_NAME",
        ProductType.SAVINGS,
        userId,
        movementBalance,
        currentBalance,
        JBH_ZERO,
        true,
        LocalDateTime.now(),
        LocalDateTime.now(),
        BigDecimal.ZERO,
        new HashMap<>());
  }

  public AccountDomainTestBuilder withCurrentBalance(final BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
    return this;
  }

  public AccountDomainTestBuilder withMovementBalance(final BigDecimal movementBalance) {
    this.movementBalance = movementBalance;
    return this;
  }

  public AccountDomainTestBuilder withUserId(final UUID userId) {
    this.userId = userId;
    return this;
  }

  public AccountDomainTestBuilder withAccountId(final AccountId accountId) {
    this.accountId = accountId;
    return this;
  }

  public static AccountDomainTestBuilder builder() {
    return new AccountDomainTestBuilder();
  }
}

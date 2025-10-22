package com.jbh.account.application.builders;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Test builder for creating AccountDomain instances with basic movement data for existing accounts
 * in the application module tests. This builder is intended for test purposes only and should not
 * be used in production code.
 */
public class AccountEntityBuilder {

  private AccountId accountId;
  private UUID userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;

  private AccountEntityBuilder() {}

  public static AccountEntityBuilder builder() {
    return new AccountEntityBuilder();
  }

  public AccountEntityBuilder withAccountId(final AccountId accountId) {
    this.accountId = accountId;
    return this;
  }

  public AccountEntityBuilder withUserId(final UUID userId) {
    this.userId = userId;
    return this;
  }

  public AccountEntityBuilder withMovementBalance(final BigDecimal movementBalance) {
    this.movementBalance = movementBalance;
    return this;
  }

  public AccountEntityBuilder withCurrentBalance(final BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
    return this;
  }

  /**
   * Builds an AccountDomain instance with basic movement data for an existing account. This method
   * uses reflection to set protected fields since it's for testing purposes.
   *
   * @return AccountDomain instance configured with the builder's data
   */
  public AccountDomain build() {
    try {
      final AccountDomain accountDomain = new AccountDomain();

      // Set fields using reflection since they're protected and in a different module
      setField(accountDomain, "id", accountId);
      setField(accountDomain, "userId", userId);
      setField(accountDomain, "movementBalance", movementBalance);
      setField(accountDomain, "currentBalance", currentBalance);

      return accountDomain;
    } catch (Exception e) {
      throw new RuntimeException("Failed to build AccountDomain for testing", e);
    }
  }

  private void setField(
      final AccountDomain accountDomain, final String fieldName, final Object value)
      throws NoSuchFieldException, IllegalAccessException {
    final var field = AccountDomain.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(accountDomain, value);
  }

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
}

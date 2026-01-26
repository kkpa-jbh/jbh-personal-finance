package com.jbh.account.application.builders;

import static com.jbh.account.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test builder for creating AccountDomain instances with basic movement data for existing accounts
 * in the application module tests. This builder is intended for test purposes only and should not
 * be used in production code.
 */
public class AccountEntityBuilder {

  private ProductId accountId;
  private UUID userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;

  private String name;
  private ProductType type;

  private AccountEntityBuilder() {}

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
  public static ProductDomain withBasicMovementForExisting(
      final ProductId accountId,
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
   * uses reflection to set protected fields since it's for testing purposes.
   *
   * @return AccountDomain instance configured with the builder's data
   */
  public ProductDomain build() {
    try {
      final ProductDomain accountDomain =
          new ProductDomain(
              accountId,
              name,
              type != null ? type : DEFAULT_ACCOUNT_TYPE,
              userId,
              JBH_ZERO,
              JBH_ZERO,
              JBH_ZERO,
              true,
              LocalDateTime.now(),
              LocalDateTime.now(),
              JBH_ZERO,
              ProductMetadata.empty());

      // Set fields using reflection since they're protected and in a different module
      setField(accountDomain, "id", accountId);
      setField(accountDomain, "userId", userId);
      setField(accountDomain, "movementBalance", movementBalance);
      setField(accountDomain, "currentBalance", currentBalance);

      return accountDomain;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to build AccountDomain for testing", e);
    }
  }

  public AccountEntityBuilder withCurrentBalance(final BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
    return this;
  }

  public AccountEntityBuilder withMovementBalance(final BigDecimal movementBalance) {
    this.movementBalance = movementBalance;
    return this;
  }

  public AccountEntityBuilder withUserId(final UUID userId) {
    this.userId = userId;
    return this;
  }

  public AccountEntityBuilder withAccountId(final ProductId accountId) {
    this.accountId = accountId;
    return this;
  }

  public static AccountEntityBuilder builder() {
    return new AccountEntityBuilder();
  }

  private void setField(
      final ProductDomain accountDomain, final String fieldName, final Object value)
      throws NoSuchFieldException, IllegalAccessException {
    final var field = ProductDomain.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(accountDomain, value);
  }

  public static AccountEntityBuilder withBuilder(
      final ProductId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final String name,
      final ProductType accountType) {
    return builder()
        .withAccountId(accountId)
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .withName(name)
        .withAccountType(accountType);
  }

  public AccountEntityBuilder withAccountType(final ProductType accountType) {
    this.type = accountType;
    return this;
  }

  public AccountEntityBuilder withName(final String name) {
    this.name = name;
    return this;
  }
}

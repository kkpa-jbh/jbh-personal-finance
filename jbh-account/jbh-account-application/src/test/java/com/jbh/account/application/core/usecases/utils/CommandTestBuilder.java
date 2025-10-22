package com.jbh.account.application.core.usecases.utils;

import com.jbh.account.application.core.vo.commands.CreateBasicAccountCommand;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test Data Factory for creating command objects with sensible defaults. Centralizes command
 * creation to make tests more maintainable when command structure changes.
 */
public class CommandTestBuilder {

  private static final String DEFAULT_ACCOUNT_NAME = "Test Account";

  /**
   * Creates a CreateBasicAccountCommand with default values and empty metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param type the account type
   * @return a CreateBasicAccountCommand
   */
  public static CreateBasicAccountCommand createBasicAccountCommand(
      final UUID userId, final String name, final AccountType type) {
    return new CreateBasicAccountCommand(userId, name, type, Map.of());
  }

  /**
   * Creates a CreateBasicAccountCommand with metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param type the account type
   * @param metadata the account metadata
   * @return a CreateBasicAccountCommand
   */
  public static CreateBasicAccountCommand createBasicAccountCommand(
      final UUID userId,
      final String name,
      final AccountType type,
      final Map<AccountMetadataKey, Object> metadata) {
    return new CreateBasicAccountCommand(userId, name, type, metadata);
  }

  /**
   * Creates a CreateBasicAccountCommand with default name and empty metadata.
   *
   * @param userId the user ID
   * @param type the account type
   * @return a CreateBasicAccountCommand
   */
  public static CreateBasicAccountCommand createBasicAccountCommand(
      final UUID userId, final AccountType type) {
    return new CreateBasicAccountCommand(userId, DEFAULT_ACCOUNT_NAME, type, Map.of());
  }

  /**
   * Creates a CreateBasicAccountCommand for a credit card account with typical metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param creditLimit the credit limit
   * @param paymentDueDay the payment due day
   * @return a CreateBasicAccountCommand configured for credit card
   */
  public static CreateBasicAccountCommand createCreditCardCommand(
      final UUID userId,
      final String name,
      final BigDecimal creditLimit,
      final Integer paymentDueDay) {
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(AccountMetadataKey.CREDIT_LIMIT, creditLimit);
    metadata.put(AccountMetadataKey.PAYMENT_DUE_DAY, paymentDueDay);
    return new CreateBasicAccountCommand(userId, name, AccountType.CREDIT_CARD, metadata);
  }

  /**
   * Creates a CreateBasicAccountCommand for an investment account with typical metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param brokerName the broker name
   * @return a CreateBasicAccountCommand configured for investment
   */
  public static CreateBasicAccountCommand createInvestmentCommand(
      final UUID userId, final String name, final String brokerName) {
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(AccountMetadataKey.BROKER_NAME, brokerName);
    return new CreateBasicAccountCommand(userId, name, AccountType.INVESTMENT, metadata);
  }
}
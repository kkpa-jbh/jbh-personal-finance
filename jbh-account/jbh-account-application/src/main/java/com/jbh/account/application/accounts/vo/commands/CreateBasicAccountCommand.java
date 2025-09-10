package com.jbh.account.application.accounts.vo.commands;

import com.jbh.account.domain.vo.AccountType;
import java.util.UUID;

public record CreateBasicAccountCommand(
    UUID userId,
    String name,
    AccountType type
) implements CommandValidator {


  @Override
  public void validate() {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Account name cannot be null or blank");
    }

    if (type == null) {
      throw new IllegalArgumentException("Account type cannot be null");
    }
  }
}

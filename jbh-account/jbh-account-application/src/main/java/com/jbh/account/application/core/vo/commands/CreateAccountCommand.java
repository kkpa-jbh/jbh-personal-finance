package com.jbh.account.application.core.vo.commands;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record CreateAccountCommand(
    UUID userId, String name, AccountType type, Map<AccountMetadataKey, Object> metadata)
    implements CommandValidator {

  // Convenience constructor for backward compatibility
  public CreateAccountCommand(final UUID userId, final String name, final AccountType type) {
    this(userId, name, type, Collections.emptyMap());
  }

  // Canonical constructor to ensure immutability

  /*
   *  WITHOUT canonical constructor - DANGEROUS!
  var metadata = new HashMap<AccountMetadataKey, Object>();
  metadata.put(CREDIT_LIMIT, 5000);
  var cmd = new CreateBasicAccountCommand(userId, "Card", CREDIT_CARD, metadata);

  metadata.put(CREDIT_LIMIT, 10000);  // ⚠️ Modifies the command's internal state!
   */
  @SuppressWarnings("PMD.UnusedAssignment")
  public CreateAccountCommand {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (name == null || name.isBlank()) {
      throw new GenericSpecificationException("Account name cannot be null or blank");
    }

    if (type == null) {
      throw new GenericSpecificationException("Account type cannot be null");
    }

    // Reassign parameters before they're assigned to fields
    if (metadata != null) {
      metadata = Map.copyOf(metadata); // Make immutable copy
    } else {
      metadata = Collections.emptyMap();
    }
  }

  @Override
  public void validate() {
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (name == null || name.isBlank()) {
      throw new GenericSpecificationException("Account name cannot be null or blank");
    }

    if (type == null) {
      throw new GenericSpecificationException("Account type cannot be null");
    }
  }
}

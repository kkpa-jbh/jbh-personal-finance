package com.jbh.account.application.core.vo.commands;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductType;
import java.util.UUID;

public record CreateAccountCommand(
    UUID userId, String name, ProductType type, ProductMetadata productMetadata)
    implements CommandValidator {

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
    if (productMetadata == null) {
      productMetadata = ProductMetadata.empty();
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

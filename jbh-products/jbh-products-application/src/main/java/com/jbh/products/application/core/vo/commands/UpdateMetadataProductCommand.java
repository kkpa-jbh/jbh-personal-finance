package com.jbh.products.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.vo.ProductMetadata;

public record UpdateMetadataProductCommand(ProductMetadata metadata) {

  public UpdateMetadataProductCommand {
    if (metadata == null || metadata.isEmpty()) {
      throw new GenericSpecificationException("Metadata cannot be null");
    }
  }
}

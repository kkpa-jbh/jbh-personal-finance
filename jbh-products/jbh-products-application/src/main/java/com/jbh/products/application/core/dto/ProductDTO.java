package com.jbh.products.application.core.dto;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.products.application.core.mappers.AccountMapper;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(builderMethodName = "notUseThisInternalBuilder")
public record ProductDTO(
    ProductId id,
    String name,
    ProductType type,
    UUID userId,
    BigDecimal movementBalance,
    BigDecimal currentBalance,
    BigDecimal netProfitBalance,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal netGrowthRate,
    ProductMetadata metadata) {

  public static ProductDTO.ProductDTOBuilder defaultBuilder(
      final UUID userId, final ProductId accountId, final String name, final ProductType type) {
    return ProductDTO.notUseThisInternalBuilder()
        .userId(userId)
        .id(accountId)
        .name(name)
        .type(type)
        .movementBalance(JBH_ZERO)
        .currentBalance(JBH_ZERO)
        .netProfitBalance(JBH_ZERO)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .netGrowthRate(JBH_ZERO)
        .metadata(ProductMetadata.empty());
  }

  public boolean isCDT() {
    return type == ProductType.CDT;
  }

  public boolean isFullyWithdrawn() {
    return metadata.findCommonMetadata().isFullyWithdrawn();
  }

  public ProductDomain toDomain() {
    return AccountMapper.toDomain(this);
  }

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return type.productTypeShouldUpdateMonthlyBalance();
  }

  public boolean isLoan() {
    return type == ProductType.LOAN;
  }
}

package com.jbh.account.application.core.dto;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(builderMethodName = "notUseThisInternalBuilder")
public record ProductDTO(
    AccountId id,
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
      final UUID userId, final AccountId accountId, final String name, final ProductType type) {
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
    return metadata.isFullyWithdrawn();
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

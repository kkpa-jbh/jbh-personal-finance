package com.jbh.account.application.core.mappers;

import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;

public final class MovementMapper {

  private MovementMapper() {}

  public static AccountMovementDomain toDomain(final MovementDTO dto) {
    if (dto == null) {
      return null;
    }

    return new AccountMovementDomain(
        dto.id(),
        dto.accountId(),
        dto.movementType(),
        CategoryMapper.toDomain(dto.category()),
        dto.movementAmount(),
        dto.movementDate(),
        dto.balanceSnapshot(),
        dto.metadata());
  }

  /**
   * Creates a MovementDTO from an AddMovementCommand.
   *
   * @param accountId The account ID
   * @param command The AddMovementCommand containing movement details
   * @return MovementDTO with the movement data
   */
  public static MovementDTO fromCommand(
      final AccountId accountId, final AddMovementCommand command) {
    BigDecimal totalAmount = command.totalAmount();
    final MovementType movementType = command.movementType();
    totalAmount = movementType == WITHDRAWAL ? totalAmount.negate() : totalAmount;

    final AccountMovementDomain newMovement =
        new AccountMovementDomain(
            accountId,
            movementType,
            command.entryDate(),
            totalAmount,
            command.balanceSnapshot(),
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withDTO(command.categoryDTO()));

    return toDTO(newMovement);
  }

  public static MovementDTO toDTO(final AccountMovementDomain domain) {
    if (domain == null) {
      return null;
    }

    return MovementDTO.builder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .movementType(domain.getMovementType())
        .category(CategoryMapper.toDTO(domain.getCategory()))
        .movementAmount(domain.getMovementAmount())
        .movementDate(domain.getMovementDate())
        .balanceSnapshot(domain.getBalanceSnapshot())
        .metadata(domain.getMetadata())
        .build();
  }

  /**
   * Creates a MovementDTO from a LiquidateAccountCommand. Liquidation movements always use
   * INVESTMENT_WITHDRAWAL category and WITHDRAWAL type.
   *
   * @param accountId The account ID
   * @param command The LiquidateAccountCommand containing liquidation details
   * @return MovementDTO with the liquidation movement data
   */
  public static MovementDTO fromCommand(
      final AccountId accountId, final LiquidateAccountCommand command) {
    final BigDecimal totalAmount = command.totalAmount().negate();
    final MovementCategoryDTO categoryDTO =
        MovementCategoryDTO.withType(ExpenseCategory.INVESTMENT_WITHDRAWAL);

    final AccountMovementMetadata metadata = AccountMovementMetadata.createEmpty();

    final AccountMovementDomain newMovement =
        new AccountMovementDomain(
            accountId,
            WITHDRAWAL,
            command.transferDate(),
            totalAmount,
            null,
            metadata,
            MovementCategoryDomain.withDTO(categoryDTO));

    return toDTO(newMovement);
  }
}

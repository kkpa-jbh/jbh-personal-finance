package com.jbh.products.application.feature.movement.mappers;

import static com.jbh.products.domain.movement.vo.MovementType.WITHDRAWAL;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.movement.commands.AddMovementCommand;
import com.jbh.products.application.feature.movement.commands.LiquidateAccountCommand;
import com.jbh.products.domain.movement.MovementCategoryDomain;
import com.jbh.products.domain.movement.MovementDomain;
import com.jbh.products.domain.movement.vo.AccountMovementMetadata;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementType;
import com.jbh.products.domain.product.vo.ProductId;
import java.math.BigDecimal;

public final class MovementMapper {

  private MovementMapper() {}

  public static MovementDomain toDomain(final MovementDTO dto) {
    if (dto == null) {
      return null;
    }

    return new MovementDomain(
        dto.id(),
        dto.accountId(),
        dto.movementType(),
        CategoryMapper.toDomain(dto.category()),
        dto.movementAmount(),
        dto.movementDate(),
        dto.balanceSnapshot(),
        dto.metadata(),
        dto.description());
  }

  /**
   * Creates a MovementDTO from an AddMovementCommand.
   *
   * @param accountId The account ID
   * @param command The AddMovementCommand containing movement details
   * @return MovementDTO with the movement data
   */
  public static MovementDTO fromCommand(
      final ProductId accountId, final AddMovementCommand command) {
    BigDecimal totalAmount = command.totalAmount();
    final MovementType movementType = command.movementType();
    totalAmount = movementType == WITHDRAWAL ? totalAmount.negate() : totalAmount;

    final MovementDomain newMovement =
        new MovementDomain(
            accountId,
            movementType,
            command.entryDate(),
            totalAmount,
            command.balanceSnapshot(),
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withDTO(command.categoryDTO()),
            command.description());

    return toDTO(newMovement);
  }

  public static MovementDTO toDTO(final MovementDomain domain) {
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
        .description(domain.getDescription())
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
      final ProductId accountId, final LiquidateAccountCommand command) {
    final BigDecimal totalAmount = command.currentBalance();
    final MovementCategoryVO categoryDTO =
        MovementCategoryVO.withType(ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);

    final AccountMovementMetadata metadata = AccountMovementMetadata.createEmpty();

    final MovementDomain newMovement =
        new MovementDomain(
            accountId,
            WITHDRAWAL,
            command.liquidatedDate(),
            totalAmount.negate(),
            BigDecimal.ZERO,
            metadata,
            MovementCategoryDomain.withDTO(categoryDTO),
            null);

    return toDTO(newMovement);
  }
}

package com.jbh.finance.application.feature.movement.mappers;

import static com.jbh.finance.domain.movement.vo.MovementType.WITHDRAWAL;

import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.commands.LiquidateProductCommand;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.movement.MovementCategoryDomain;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
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
   * @param accountId The productDTO ID
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
            MovementMetadata.createEmpty(),
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
        .accountId(domain.getProductId())
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
   * @param accountId The productDTO ID
   * @param command The LiquidateAccountCommand containing liquidation details
   * @return MovementDTO with the liquidation movement data
   */
  public static MovementDTO fromCommand(
      final ProductId accountId, final LiquidateProductCommand command) {
    final BigDecimal totalAmount = command.currentBalance();
    final MovementCategoryVO categoryDTO =
        MovementCategoryVO.withType(ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);

    final MovementMetadata metadata = MovementMetadata.createEmpty();

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

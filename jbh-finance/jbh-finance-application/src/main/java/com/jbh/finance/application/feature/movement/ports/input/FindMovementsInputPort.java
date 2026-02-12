package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.usecases.FindMovementsUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FindMovementsInputPort implements FindMovementsUseCase {

  private static final int DEFAULT_MONTHS_BACK = 3;

  private final MovementLifecycleService movementService;
  private final ProductLifecycleService productsService;

  public FindMovementsInputPort(
      final MovementLifecycleService movementService,
      final ProductLifecycleService productsService) {
    this.movementService = movementService;
    this.productsService = productsService;
  }

  @Override
  public List<MovementDTO> findMovementsByProduct(
      final UUID userId, final ProductId productId, final Integer monthsBack)
      throws BusinessException {

    // Validations
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      throw new GenericSpecificationException("Product ID cannot be null");
    }

    // Verify product exists and belongs to user
    final ProductDTO product = productsService.findByUserAndProductId(userId, productId);

    // Calculate date range
    final int months = (monthsBack != null && monthsBack > 0) ? monthsBack : DEFAULT_MONTHS_BACK;
    final LocalDate endDate = calculateEndOfCurrentMonth();
    final LocalDate startDate = calculateStartDate(endDate, months);

    // Query movements within period
    final List<MovementDTO> movements =
        movementService.getByUserAndProductIdWithinPeriod(userId, productId, startDate, endDate);

    // Sort descending by movement date (newest first)
    return movements.stream()
        .sorted(Comparator.comparing(MovementDTO::movementDate).reversed())
        .toList();
  }

  private LocalDate calculateEndOfCurrentMonth() {
    final YearMonth currentMonth = YearMonth.now();
    return currentMonth.atEndOfMonth();
  }

  private LocalDate calculateStartDate(final LocalDate endDate, final int monthsBack) {
    final YearMonth endMonth = YearMonth.from(endDate);
    final YearMonth startMonth = endMonth.minusMonths(monthsBack - 1);
    return startMonth.atDay(1);
  }
}

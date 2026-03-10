package com.jbh.finance.application.feature.movement.policy;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import java.time.YearMonth;

/**
 * Business rules for movement removal eligibility.
 *
 * <p>This policy centralizes the logic for determining whether a movement can be removed. This
 * allows the same rule to be reused across different use cases (queries, removal validation, etc.)
 * without duplication.
 */
@Deprecated(since = "See canBeRemoved from MovementDTO")
public class MovementRemovalPolicy {

  /**
   * Determines if a movement can be removed based on business rules.
   *
   * <p>Current rule: A movement can only be removed if it was created in the current month.
   *
   * @param movement the movement to check
   * @return true if the movement can be removed, false otherwise
   */
  public boolean canBeRemoved(final MovementDTO movement) {
    if (movement.createdAt() == null) {
      return false;
    }

    final YearMonth currentMonth = YearMonth.now();
    final YearMonth createdMonth = YearMonth.from(movement.createdAt());

    return currentMonth.equals(createdMonth);
  }
}

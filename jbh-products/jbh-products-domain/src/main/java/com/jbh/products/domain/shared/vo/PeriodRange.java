package com.jbh.products.domain.shared.vo;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PeriodRange {

  private final YearMonth startPeriod;
  private final YearMonth endPeriod;
  private final boolean endPeriodExclusive;

  private PeriodRange(
      final YearMonth startPeriod,
      final YearMonth inputEndPeriod,
      final boolean endPeriodExclusive) {
    Objects.requireNonNull(startPeriod, "Start period cannot be null");
    Objects.requireNonNull(inputEndPeriod, "End period cannot be null");

    if (startPeriod.isAfter(inputEndPeriod)) {
      throw new IllegalArgumentException("Start period must be before or equal to end period");
    }

    this.startPeriod = startPeriod;
    this.endPeriod = inputEndPeriod;
    this.endPeriodExclusive = endPeriodExclusive;
  }

  public static PeriodRange inclusive(final YearMonth startPeriod, final YearMonth endPeriod) {
    return new PeriodRange(startPeriod, endPeriod, false);
  }

  public static PeriodRange exclusive(final YearMonth startPeriod, final YearMonth endPeriod) {
    return new PeriodRange(startPeriod, endPeriod, true);
  }

  /**
   * Autoexclusive if the end period is the same of today
   *
   * @param startPeriod
   * @param endPeriod
   * @param today
   * @return
   */
  public static PeriodRange autoExclusive(
      final YearMonth startPeriod, final YearMonth endPeriod, final YearMonth today) {
    final boolean shouldBeExclusive = endPeriod.equals(today);
    return new PeriodRange(startPeriod, endPeriod, shouldBeExclusive);
  }

  public static PeriodRange from(final YearMonth startPeriod) {
    return new PeriodRange(startPeriod, YearMonth.now(), false);
  }

  public static PeriodRange upTo(final YearMonth endPeriod) {
    return new PeriodRange(YearMonth.now(), endPeriod, true);
  }

  public YearMonth getStartPeriod() {
    return startPeriod;
  }

  public YearMonth getEndPeriodExclusive() {
    if (this.endPeriodExclusive) {
      return endPeriod.minusMonths(1);
    }
    return getEndPeriod();
  }

  public YearMonth getEndPeriod() {
    return endPeriod;
  }

  public boolean isEndPeriodExclusive() {
    return endPeriodExclusive;
  }

  public List<YearMonth> getMonths() {
    final List<YearMonth> months = new ArrayList<>();
    YearMonth current = startPeriod;

    while (contains(current)) {
      months.add(current);
      current = current.plusMonths(1);
    }

    return months;
  }

  public boolean contains(final YearMonth period) {
    Objects.requireNonNull(period, "Period cannot be null");

    final boolean afterOrEqualStart = !period.isBefore(startPeriod);
    final boolean beforeEnd =
        endPeriodExclusive ? period.isBefore(endPeriod) : !period.isAfter(endPeriod);

    return afterOrEqualStart && beforeEnd;
  }

  public boolean overlaps(final PeriodRange other) {
    Objects.requireNonNull(other, "Other period range cannot be null");

    return !this.startPeriod.isAfter(other.getEffectiveEndPeriod())
        && !this.getEffectiveEndPeriod().isBefore(other.startPeriod);
  }

  private YearMonth getEffectiveEndPeriod() {
    return endPeriodExclusive ? endPeriod.minusMonths(1) : endPeriod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPeriod, endPeriod, endPeriodExclusive);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PeriodRange that = (PeriodRange) o;
    return endPeriodExclusive == that.endPeriodExclusive
        && Objects.equals(startPeriod, that.startPeriod)
        && Objects.equals(endPeriod, that.endPeriod);
  }

  @Override
  public String toString() {
    final String endBracket = endPeriodExclusive ? ")" : "]";
    return "[" + startPeriod + ", " + endPeriod + endBracket;
  }
}

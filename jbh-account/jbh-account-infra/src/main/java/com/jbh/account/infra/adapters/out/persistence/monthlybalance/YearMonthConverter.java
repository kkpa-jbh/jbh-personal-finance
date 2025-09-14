package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

@Converter(autoApply = false)
public class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

  @Override
  public LocalDate convertToDatabaseColumn(YearMonth yearMonth) {
    return yearMonth != null ? yearMonth.atDay(1) : null;
  }

  @Override
  public YearMonth convertToEntityAttribute(LocalDate localDate) {
    return localDate != null ? YearMonth.from(localDate) : null;
  }
}
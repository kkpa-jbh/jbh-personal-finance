package com.jbh.account.domain.vo;

import com.jbh.account.domain.utils.JbhStringUtils;
import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductType {
  SAVINGS("Savings", "Ahorros"),
  REAL_ESTATE_INVESTMENT("Real Estate Investment", "Inversión Inmobiliaria"),
  LOAN("Loan", "Préstamo"),
  CREDIT_CARD("Credit Card", "Tarjeta de Crédito"),
  INVESTMENT("Investment", "Inversión"),
  CDT("Certificate of Deposit", "CDT")
  ;

  private static final List<ProductType> ADDING_MOVEMENTS_PRODUCTS_ALLOWED =
      List.of(SAVINGS, CREDIT_CARD, INVESTMENT, CDT);
  private final String translationKey;

  ProductType(final String eng, final String es) {
    this.translationKey = JbhStringUtils.buildJsonMessage(eng, es);
  }

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return this == SAVINGS || this == CREDIT_CARD || this == INVESTMENT;
  }

  public List<ProductType> addingMovementsProductsAllowed() {
    return ADDING_MOVEMENTS_PRODUCTS_ALLOWED;
  }

  public String getTranslationKey() {
    return translationKey;
  }
}

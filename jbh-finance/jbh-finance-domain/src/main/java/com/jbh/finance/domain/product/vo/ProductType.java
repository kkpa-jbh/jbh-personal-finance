package com.jbh.finance.domain.product.vo;

import com.jbh.commons.util.JbhStringUtils;
import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductType {
  SAVINGS("Savings", "Ahorros"),
  REAL_ESTATE_INVESTMENT("Real Estate Investment", "Inversión Inmobiliaria"),
  LOAN("Loan", "Préstamo"),
  CREDIT_CARD("Credit Card", "Tarjeta de Crédito"),
  INVESTMENT("Investment", "Inversión"),
  CDT("Certificate of Deposit (CDT)", "CDT");

  private static final List<ProductType> ADDING_MOVEMENTS_PRODUCTS_ALLOWED =
      List.of(SAVINGS, CREDIT_CARD, INVESTMENT, CDT);
  private static final List<ProductType> STATEMENT_IMPORT_PRODUCTS_ALLOWED =
      List.of(SAVINGS, CREDIT_CARD);
  private final String translationKey;

  ProductType(final String eng, final String es) {
    this.translationKey = JbhStringUtils.buildJsonMessage(eng, es);
  }

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return this == SAVINGS || this == CREDIT_CARD || this == INVESTMENT;
  }

  public boolean allowsStatementImport() {
    return STATEMENT_IMPORT_PRODUCTS_ALLOWED.contains(this);
  }

  public List<ProductType> addingMovementsProductsAllowed() {
    return ADDING_MOVEMENTS_PRODUCTS_ALLOWED;
  }

  public String getTranslationKey() {
    return translationKey;
  }
}

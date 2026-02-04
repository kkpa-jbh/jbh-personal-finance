package com.jbh.products.infra.adapters.in.rest.vo;

import com.jbh.products.domain.vo.CategorySource;
import com.jbh.products.domain.vo.CategoryType;
import com.jbh.products.domain.vo.ExpenseCategory;
import com.jbh.products.domain.vo.IncomeCategory;
import java.util.Arrays;
import java.util.List;

public record CategoryDTO(String name, String translationKey, CategorySource source) {

  public static CategoryDTO from(final CategoryType category) {
    return new CategoryDTO(
        category.getTypeName(), category.getTranslationsKey(), category.getSource());
  }

  public static List<CategoryDTO> allExpenseCategories() {
    return Arrays.stream(ExpenseCategory.values()).map(CategoryDTO::from).toList();
  }

  public static List<CategoryDTO> allIncomeCategories() {
    return Arrays.stream(IncomeCategory.values()).map(CategoryDTO::from).toList();
  }
}

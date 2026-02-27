package com.jbh.finance.domain.category.vo;

public record CategoryTypeVO(CategorySourceVO source, String alias) {

  public CategorySourceVO getSource() {
    return source;
  }

  public String getAlias() {
    return alias;
  }
}

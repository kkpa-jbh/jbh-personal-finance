package com.jbh.finance.domain.shared.vo;

public enum EntityOperationVO {
  ADD,
  REMOVE,
  ;

  public boolean toRemove() {
    return this == REMOVE;
  }

  public boolean toAdd() {
    return this == ADD;
  }
}

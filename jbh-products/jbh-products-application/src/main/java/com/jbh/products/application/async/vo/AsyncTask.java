package com.jbh.products.application.async.vo;

import java.util.Map;

public record AsyncTask(AsyncTaskType type, Map<String, Object> metadata) {

  @Override
  public String toString() {
    return "**"
        + "type="
        + type
        + ", metadata="
        + metadata
        + " On thread: "
        + Thread.currentThread().getName();
  }
}

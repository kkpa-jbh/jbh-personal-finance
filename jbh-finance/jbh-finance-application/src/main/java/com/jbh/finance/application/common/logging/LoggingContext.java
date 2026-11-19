package com.jbh.finance.application.common.logging;

import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;

public final class LoggingContext {

  public static final String USER_ID = "userId";
  public static final String PRODUCT_ID = "productId";
  public static final String MOVEMENT_ID = "movementId";
  public static final String MODULE = "module";

  private LoggingContext() {
    // Utility class
  }

  public static String getMovementId() {
    return MDC.get(MOVEMENT_ID);
  }

  public static void setMovementId(final String movementId) {
    MDC.put(MOVEMENT_ID, movementId);
  }

  public static void setMovementId(final UUID movementId) {
    setMovementId(movementId != null ? movementId.toString() : null);
  }

  public static String getModule() {
    return MDC.get(MODULE);
  }

  public static void setModule(final String module) {
    MDC.put(MODULE, module);
  }

  public static <T> T withContext(final Runnable action) {
    try {
      action.run();
      return null;
    } finally {
      clear();
    }
  }

  public static void clear() {
    MDC.clear();
  }

  public static <T> T withContext(final Supplier<T> action) {
    try {
      return action.get();
    } finally {
      clear();
    }
  }

  public static void remove(final String key) {
    MDC.remove(key);
  }

  public static void withUserId(final String userId, final Runnable action) {
    withUserId(
        userId,
        () -> {
          action.run();
          return null;
        });
  }

  public static <T> T withUserId(final String userId, final Supplier<T> action) {
    final String previousUserId = getUserId();
    try {
      setUserId(userId);
      return action.get();
    } finally {
      if (previousUserId != null) {
        setUserId(previousUserId);
      } else {
        remove(USER_ID);
      }
    }
  }

  public static String getUserId() {
    return MDC.get(USER_ID);
  }

  public static void setUserId(final String userId) {
    MDC.put(USER_ID, userId);
  }

  public static void setUserId(final UUID userId) {
    setUserId(userId != null ? userId.toString() : null);
  }

  public static void withAccountId(final String accountId, final Runnable action) {
    withAccountId(
        accountId,
        () -> {
          action.run();
          return null;
        });
  }

  public static <T> T withAccountId(final String accountId, final Supplier<T> action) {
    final String previousAccountId = getProductId();
    try {
      setProductId(accountId);
      return action.get();
    } finally {
      if (previousAccountId != null) {
        setProductId(previousAccountId);
      } else {
        remove(PRODUCT_ID);
      }
    }
  }

  public static String getProductId() {
    return MDC.get(PRODUCT_ID);
  }

  public static void setProductId(final String productId) {
    MDC.put(PRODUCT_ID, productId);
  }

  public static void setAccountId(final UUID accountId) {
    setProductId(accountId != null ? accountId.toString() : null);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String userId;
    private String accountId;
    private String movementId;
    private String module;

    public Builder userId(final String userId) {
      this.userId = userId;
      return this;
    }

    public Builder userId(final UUID userId) {
      this.userId = userId != null ? userId.toString() : null;
      return this;
    }

    public Builder accountId(final String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder accountId(final UUID accountId) {
      this.accountId = accountId != null ? accountId.toString() : null;
      return this;
    }

    public Builder movementId(final String movementId) {
      this.movementId = movementId;
      return this;
    }

    public Builder movementId(final UUID movementId) {
      this.movementId = movementId != null ? movementId.toString() : null;
      return this;
    }

    public Builder module(final String module) {
      this.module = module;
      return this;
    }

    public void execute(final Runnable action) {
      execute(
          () -> {
            action.run();
            return null;
          });
    }

    public <T> T execute(final Supplier<T> action) {
      apply();
      try {
        return action.get();
      } finally {
        clear();
      }
    }

    public void apply() {
      if (userId != null) {
        setUserId(userId);
      }
      if (accountId != null) {
        setProductId(accountId);
      }
      if (movementId != null) {
        setMovementId(movementId);
      }
      if (module != null) {
        setModule(module);
      }
    }
  }
}

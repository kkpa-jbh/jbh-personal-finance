package com.jbh.products.application.common.logging;

import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;

public final class LoggingContext {

  public static final String TRACKING_ID = "trackingId";
  public static final String USER_ID = "userId";
  public static final String ACCOUNT_ID = "accountId";
  public static final String MOVEMENT_ID = "movementId";
  public static final String MODULE = "module";
  public static final String REQUEST_ID = "requestId";

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

  public static String getRequestId() {
    return MDC.get(REQUEST_ID);
  }

  public static void setRequestId(final String requestId) {
    MDC.put(REQUEST_ID, requestId);
  }

  public static void setRequestId(final UUID requestId) {
    setRequestId(requestId != null ? requestId.toString() : null);
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

  public static void withTrackingId(final String trackingId, final Runnable action) {
    withTrackingId(
        trackingId,
        () -> {
          action.run();
          return null;
        });
  }

  public static <T> T withTrackingId(final String trackingId, final Supplier<T> action) {
    final String previousTrackingId = getTrackingId();
    try {
      setTrackingId(trackingId);
      return action.get();
    } finally {
      if (previousTrackingId != null) {
        setTrackingId(previousTrackingId);
      } else {
        remove(TRACKING_ID);
      }
    }
  }

  public static String getTrackingId() {
    return MDC.get(TRACKING_ID);
  }

  public static void setTrackingId(final String trackingId) {
    MDC.put(TRACKING_ID, trackingId);
  }

  public static void setTrackingId(final UUID trackingId) {
    setTrackingId(trackingId != null ? trackingId.toString() : null);
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
    final String previousAccountId = getAccountId();
    try {
      setAccountId(accountId);
      return action.get();
    } finally {
      if (previousAccountId != null) {
        setAccountId(previousAccountId);
      } else {
        remove(ACCOUNT_ID);
      }
    }
  }

  public static String getAccountId() {
    return MDC.get(ACCOUNT_ID);
  }

  public static void setAccountId(final String accountId) {
    MDC.put(ACCOUNT_ID, accountId);
  }

  public static void setAccountId(final UUID accountId) {
    setAccountId(accountId != null ? accountId.toString() : null);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String trackingId;
    private String userId;
    private String accountId;
    private String movementId;
    private String module;
    private String requestId;

    public Builder trackingId(final String trackingId) {
      this.trackingId = trackingId;
      return this;
    }

    public Builder trackingId(final UUID trackingId) {
      this.trackingId = trackingId != null ? trackingId.toString() : null;
      return this;
    }

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

    public Builder requestId(final String requestId) {
      this.requestId = requestId;
      return this;
    }

    public Builder requestId(final UUID requestId) {
      this.requestId = requestId != null ? requestId.toString() : null;
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
      if (trackingId != null) {
        setTrackingId(trackingId);
      }
      if (userId != null) {
        setUserId(userId);
      }
      if (accountId != null) {
        setAccountId(accountId);
      }
      if (movementId != null) {
        setMovementId(movementId);
      }
      if (module != null) {
        setModule(module);
      }
      if (requestId != null) {
        setRequestId(requestId);
      }
    }
  }
}

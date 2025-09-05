package com.jbh.account_app.common.logging;

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

  public static String getTrackingId() {
    return MDC.get(TRACKING_ID);
  }

  public static void setTrackingId(String trackingId) {
    MDC.put(TRACKING_ID, trackingId);
  }

  public static void setTrackingId(UUID trackingId) {
    setTrackingId(trackingId != null ? trackingId.toString() : null);
  }

  public static String getUserId() {
    return MDC.get(USER_ID);
  }

  public static void setUserId(String userId) {
    MDC.put(USER_ID, userId);
  }

  public static void setUserId(UUID userId) {
    setUserId(userId != null ? userId.toString() : null);
  }

  public static String getAccountId() {
    return MDC.get(ACCOUNT_ID);
  }

  public static void setAccountId(String accountId) {
    MDC.put(ACCOUNT_ID, accountId);
  }

  public static void setAccountId(UUID accountId) {
    setAccountId(accountId != null ? accountId.toString() : null);
  }

  public static String getMovementId() {
    return MDC.get(MOVEMENT_ID);
  }

  public static void setMovementId(String movementId) {
    MDC.put(MOVEMENT_ID, movementId);
  }

  public static void setMovementId(UUID movementId) {
    setMovementId(movementId != null ? movementId.toString() : null);
  }

  public static String getModule() {
    return MDC.get(MODULE);
  }

  public static void setModule(String module) {
    MDC.put(MODULE, module);
  }

  public static String getRequestId() {
    return MDC.get(REQUEST_ID);
  }

  public static void setRequestId(String requestId) {
    MDC.put(REQUEST_ID, requestId);
  }

  public static void setRequestId(UUID requestId) {
    setRequestId(requestId != null ? requestId.toString() : null);
  }

  public static void clear() {
    MDC.clear();
  }

  public static void remove(String key) {
    MDC.remove(key);
  }

  public static <T> T withContext(Runnable action) {
    try {
      action.run();
      return null;
    } finally {
      clear();
    }
  }

  public static <T> T withContext(Supplier<T> action) {
    try {
      return action.get();
    } finally {
      clear();
    }
  }

  public static <T> T withTrackingId(String trackingId, Supplier<T> action) {
    String previousTrackingId = getTrackingId();
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

  public static void withTrackingId(String trackingId, Runnable action) {
    withTrackingId(trackingId, () -> {
      action.run();
      return null;
    });
  }

  public static <T> T withUserId(String userId, Supplier<T> action) {
    String previousUserId = getUserId();
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

  public static void withUserId(String userId, Runnable action) {
    withUserId(userId, () -> {
      action.run();
      return null;
    });
  }

  public static <T> T withAccountId(String accountId, Supplier<T> action) {
    String previousAccountId = getAccountId();
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

  public static void withAccountId(String accountId, Runnable action) {
    withAccountId(accountId, () -> {
      action.run();
      return null;
    });
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

    public Builder trackingId(String trackingId) {
      this.trackingId = trackingId;
      return this;
    }

    public Builder trackingId(UUID trackingId) {
      this.trackingId = trackingId != null ? trackingId.toString() : null;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder userId(UUID userId) {
      this.userId = userId != null ? userId.toString() : null;
      return this;
    }

    public Builder accountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder accountId(UUID accountId) {
      this.accountId = accountId != null ? accountId.toString() : null;
      return this;
    }

    public Builder movementId(String movementId) {
      this.movementId = movementId;
      return this;
    }

    public Builder movementId(UUID movementId) {
      this.movementId = movementId != null ? movementId.toString() : null;
      return this;
    }

    public Builder module(String module) {
      this.module = module;
      return this;
    }

    public Builder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public Builder requestId(UUID requestId) {
      this.requestId = requestId != null ? requestId.toString() : null;
      return this;
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

    public <T> T execute(Supplier<T> action) {
      apply();
      try {
        return action.get();
      } finally {
        clear();
      }
    }

    public void execute(Runnable action) {
      execute(() -> {
        action.run();
        return null;
      });
    }
  }
}
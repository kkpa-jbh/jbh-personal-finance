package com.jbh.notification.contracts;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed accessor for email template metadata.
 * Provides type-safe getters/setters for template-specific fields
 * and converts to/from the generic Map used in notifications.
 */
public final class EmailMetadata {

    private static final String KEY_TEAM_ID = "teamId";
    private static final String KEY_TEAM_NAME = "teamName";
    private static final String KEY_INVITER_NAME = "inviterName";
    private static final String KEY_LOCALE = "locale";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_RESET_TOKEN = "resetToken";
    private static final String KEY_VERIFICATION_CODE = "verificationCode";

    private static final String DEFAULT_LOCALE = "en";
    private static final String DEFAULT_TEAM_NAME = "Unknown Team";
    private static final String DEFAULT_INVITER_NAME = "A team member";

    private final Map<String, Object> data;

    private EmailMetadata(final Map<String, Object> data) {
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    public static EmailMetadata fromMap(final Map<String, Object> metadata) {
        return new EmailMetadata(metadata);
    }

    public static EmailMetadata empty() {
        return new EmailMetadata(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    public String getTeamId() {
        return getString(KEY_TEAM_ID, "");
    }

    public EmailMetadata withTeamId(final String teamId) {
        data.put(KEY_TEAM_ID, teamId);
        return this;
    }

    public String getTeamName() {
        return getString(KEY_TEAM_NAME, DEFAULT_TEAM_NAME);
    }

    public EmailMetadata withTeamName(final String teamName) {
        data.put(KEY_TEAM_NAME, teamName);
        return this;
    }

    public String getInviterName() {
        return getString(KEY_INVITER_NAME, DEFAULT_INVITER_NAME);
    }

    public EmailMetadata withInviterName(final String inviterName) {
        data.put(KEY_INVITER_NAME, inviterName);
        return this;
    }

    public String getLocale() {
        return getString(KEY_LOCALE, DEFAULT_LOCALE);
    }

    public EmailMetadata withLocale(final String locale) {
        data.put(KEY_LOCALE, locale);
        return this;
    }

    public String getUserName() {
        return getString(KEY_USER_NAME, "");
    }

    public EmailMetadata withUserName(final String userName) {
        data.put(KEY_USER_NAME, userName);
        return this;
    }

    public String getResetToken() {
        return getString(KEY_RESET_TOKEN, "");
    }

    public EmailMetadata withResetToken(final String resetToken) {
        data.put(KEY_RESET_TOKEN, resetToken);
        return this;
    }

    public String getVerificationCode() {
        return getString(KEY_VERIFICATION_CODE, "");
    }

    public EmailMetadata withVerificationCode(final String verificationCode) {
        data.put(KEY_VERIFICATION_CODE, verificationCode);
        return this;
    }

    public Object get(final String key) {
        return data.get(key);
    }

    public EmailMetadata with(final String key, final Object value) {
        data.put(key, value);
        return this;
    }

    private String getString(final String key, final String defaultValue) {
        final Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    public static final class Builder {
        private final Map<String, Object> data = new HashMap<>();

        private Builder() {
        }

        public Builder teamId(final String teamId) {
            data.put(KEY_TEAM_ID, teamId);
            return this;
        }

        public Builder teamName(final String teamName) {
            data.put(KEY_TEAM_NAME, teamName);
            return this;
        }

        public Builder inviterName(final String inviterName) {
            data.put(KEY_INVITER_NAME, inviterName);
            return this;
        }

        public Builder locale(final String locale) {
            data.put(KEY_LOCALE, locale);
            return this;
        }

        public Builder userName(final String userName) {
            data.put(KEY_USER_NAME, userName);
            return this;
        }

        public Builder resetToken(final String resetToken) {
            data.put(KEY_RESET_TOKEN, resetToken);
            return this;
        }

        public Builder verificationCode(final String verificationCode) {
            data.put(KEY_VERIFICATION_CODE, verificationCode);
            return this;
        }

        public Builder custom(final String key, final Object value) {
            data.put(key, value);
            return this;
        }

        public EmailMetadata build() {
            return new EmailMetadata(data);
        }
    }
}

package com.jbh.notification.infra.email;

import com.jbh.notification.contracts.email.EmailMetadataKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Typed accessor for email template metadata.
 * Provides type-safe getters for template-specific fields
 * and converts from the generic Map used in notifications.
 * This is an internal class for parsing/reading metadata on the receiving side.
 */
public final class EmailMetadata {

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

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    public String getTeamId() {
        return getString(EmailMetadataKey.TEAM_ID, "");
    }

    private String getString(final EmailMetadataKey key, final String defaultValue) {
        final Object value = data.get(key.getKey());
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    public String getTeamName() {
        return getString(EmailMetadataKey.TEAM_NAME, DEFAULT_TEAM_NAME);
    }

    public String getInviterName() {
        return getString(EmailMetadataKey.INVITER_NAME, DEFAULT_INVITER_NAME);
    }

    public String getLocale() {
        return getString(EmailMetadataKey.LOCALE, DEFAULT_LOCALE);
    }

    public String getUserName() {
        return getString(EmailMetadataKey.USER_NAME, "");
    }

    public String getResetToken() {
        return getString(EmailMetadataKey.RESET_TOKEN, "");
    }

    public String getVerificationCode() {
        return getString(EmailMetadataKey.VERIFICATION_CODE, "");
    }

    public Object get(final String key) {
        return data.get(key);
    }
}

package com.jbh.notification.contracts.email.templates;

import com.jbh.notification.contracts.email.EmailMetadataKey;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.EmailTemplateMetadata;
import java.util.HashMap;
import java.util.Map;

public final class PasswordResetMetadata implements EmailTemplateMetadata {

    private final String userName;
    private final String resetToken;
    private final String locale;

    private PasswordResetMetadata(final Builder builder) {
        this.userName = builder.userName;
        this.resetToken = builder.resetToken;
        this.locale = builder.locale;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EmailTemplate getEmailTemplate() {
        return EmailTemplate.PASSWORD_RESET;
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(EmailMetadataKey.USER_NAME.getKey(), userName);
        map.put(EmailMetadataKey.RESET_TOKEN.getKey(), resetToken);
        if (locale != null) {
            map.put(EmailMetadataKey.LOCALE.getKey(), locale);
        }
        return map;
    }

    public String getUserName() {
        return userName;
    }

    public String getResetToken() {
        return resetToken;
    }

    public String getLocale() {
        return locale;
    }

    public static final class Builder {
        private String userName;
        private String resetToken;
        private String locale;

        private Builder() {}

        public Builder userName(final String userName) {
            this.userName = userName;
            return this;
        }

        public Builder resetToken(final String resetToken) {
            this.resetToken = resetToken;
            return this;
        }

        public Builder locale(final String locale) {
            this.locale = locale;
            return this;
        }

        public PasswordResetMetadata build() {
            return new PasswordResetMetadata(this);
        }
    }
}

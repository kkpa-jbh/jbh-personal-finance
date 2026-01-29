package com.jbh.notification.contracts.email.templates;

import com.jbh.notification.contracts.email.EmailMetadataKey;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.EmailTemplateMetadata;
import java.util.HashMap;
import java.util.Map;

public final class WelcomeMetadata implements EmailTemplateMetadata {

    private final String userName;
    private final String locale;

    private WelcomeMetadata(final Builder builder) {
        this.userName = builder.userName;
        this.locale = builder.locale;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EmailTemplate getEmailTemplate() {
        return EmailTemplate.WELCOME;
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(EmailMetadataKey.USER_NAME.getKey(), userName);
        if (locale != null) {
            map.put(EmailMetadataKey.LOCALE.getKey(), locale);
        }
        return map;
    }

    public String getUserName() {
        return userName;
    }

    public String getLocale() {
        return locale;
    }

    public static final class Builder {
        private String userName;
        private String locale;

        private Builder() {}

        public Builder userName(final String userName) {
            this.userName = userName;
            return this;
        }

        public Builder locale(final String locale) {
            this.locale = locale;
            return this;
        }

        public WelcomeMetadata build() {
            return new WelcomeMetadata(this);
        }
    }
}

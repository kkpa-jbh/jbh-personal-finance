package com.jbh.notification.contracts.email.templates;

import com.jbh.notification.contracts.email.EmailMetadataKey;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.EmailTemplateMetadata;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamInvitationMetadata implements EmailTemplateMetadata {

    private final UUID teamId;
    private final String teamName;
    private final String inviterName;
    private final String locale;

    private TeamInvitationMetadata(final Builder builder) {
        this.teamId = builder.teamId;
        this.teamName = builder.teamName;
        this.inviterName = builder.inviterName;
        this.locale = builder.locale;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EmailTemplate getEmailTemplate() {
        return EmailTemplate.TEAM_INVITATION;
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(EmailMetadataKey.TEAM_ID.getKey(), teamId);
        map.put(EmailMetadataKey.TEAM_NAME.getKey(), teamName);
        map.put(EmailMetadataKey.INVITER_NAME.getKey(), inviterName);
        if (locale != null) {
            map.put(EmailMetadataKey.LOCALE.getKey(), locale);
        }
        return map;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getInviterName() {
        return inviterName;
    }

    public String getLocale() {
        return locale;
    }

    public static final class Builder {
        private UUID teamId;
        private String teamName;
        private String inviterName;
        private String locale;

        private Builder() {}

        public Builder teamId(final UUID teamId) {
            this.teamId = teamId;
            return this;
        }

        public Builder teamName(final String teamName) {
            this.teamName = teamName;
            return this;
        }

        public Builder inviterName(final String inviterName) {
            this.inviterName = inviterName;
            return this;
        }

        public Builder locale(final String locale) {
            this.locale = locale;
            return this;
        }

        public TeamInvitationMetadata build() {
            return new TeamInvitationMetadata(this);
        }
    }
}

package com.jbh.notification.contracts.email;

import com.jbh.notification.contracts.common.MetadataKey;

public enum EmailMetadataKey implements MetadataKey {

    TEAM_ID("teamId"),
    TEAM_NAME("teamName"),
    INVITER_NAME("inviterName"),
    LOCALE("locale"),
    USER_NAME("userName"),
    RESET_TOKEN("resetToken"),
    VERIFICATION_CODE("verificationCode");

    private final String key;

    EmailMetadataKey(final String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}

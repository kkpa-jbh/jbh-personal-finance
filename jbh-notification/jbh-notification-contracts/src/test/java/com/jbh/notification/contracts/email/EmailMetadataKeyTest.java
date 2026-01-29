package com.jbh.notification.contracts.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailMetadataKeyTest {

    @Test
    void shouldReturnCorrectKeyForTeamId() {
        assertThat(EmailMetadataKey.TEAM_ID.getKey()).isEqualTo("teamId");
    }

    @Test
    void shouldReturnCorrectKeyForTeamName() {
        assertThat(EmailMetadataKey.TEAM_NAME.getKey()).isEqualTo("teamName");
    }

    @Test
    void shouldReturnCorrectKeyForInviterName() {
        assertThat(EmailMetadataKey.INVITER_NAME.getKey()).isEqualTo("inviterName");
    }

    @Test
    void shouldReturnCorrectKeyForLocale() {
        assertThat(EmailMetadataKey.LOCALE.getKey()).isEqualTo("locale");
    }

    @Test
    void shouldReturnCorrectKeyForUserName() {
        assertThat(EmailMetadataKey.USER_NAME.getKey()).isEqualTo("userName");
    }

    @Test
    void shouldReturnCorrectKeyForResetToken() {
        assertThat(EmailMetadataKey.RESET_TOKEN.getKey()).isEqualTo("resetToken");
    }

    @Test
    void shouldReturnCorrectKeyForVerificationCode() {
        assertThat(EmailMetadataKey.VERIFICATION_CODE.getKey()).isEqualTo("verificationCode");
    }

    @Test
    void shouldHaveSevenEnumValues() {
        assertThat(EmailMetadataKey.values()).hasSize(7);
    }
}

package com.jbh.notification.contracts.email.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PasswordResetMetadataTest {

    @Test
    void shouldBuildMetadataWithAllRequiredFields() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder()
            .userName("john.doe")
            .resetToken("token123")
            .build();

        assertThat(metadata.getUserName()).isEqualTo("john.doe");
        assertThat(metadata.getResetToken()).isEqualTo("token123");
        assertThat(metadata.getLocale()).isNull();
    }

    @Test
    void shouldBuildMetadataWithOptionalLocale() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder()
            .userName("john.doe")
            .resetToken("token123")
            .locale("fr")
            .build();

        assertThat(metadata.getLocale()).isEqualTo("fr");
    }

    @Test
    void shouldReturnCorrectEmailTemplate() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder()
            .userName("john.doe")
            .resetToken("token123")
            .build();

        assertThat(metadata.getEmailTemplate()).isEqualTo(EmailTemplate.PASSWORD_RESET);
    }

    @Test
    void shouldConvertToMapWithoutLocale() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder()
            .userName("john.doe")
            .resetToken("token123")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(2);
        assertThat(map.get("userName")).isEqualTo("john.doe");
        assertThat(map.get("resetToken")).isEqualTo("token123");
    }

    @Test
    void shouldConvertToMapWithLocale() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder()
            .userName("john.doe")
            .resetToken("token123")
            .locale("fr")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(3);
        assertThat(map.get("locale")).isEqualTo("fr");
    }

    @Test
    void shouldAllowBuildingWithNullFields() {
        final PasswordResetMetadata metadata = PasswordResetMetadata.builder().build();

        assertThat(metadata.getUserName()).isNull();
        assertThat(metadata.getResetToken()).isNull();
    }
}

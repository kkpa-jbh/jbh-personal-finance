package com.jbh.notification.contracts.email.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WelcomeMetadataTest {

    @Test
    void shouldBuildMetadataWithRequiredField() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder()
            .userName("john.doe")
            .build();

        assertThat(metadata.getUserName()).isEqualTo("john.doe");
        assertThat(metadata.getLocale()).isNull();
    }

    @Test
    void shouldBuildMetadataWithOptionalLocale() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder()
            .userName("john.doe")
            .locale("de")
            .build();

        assertThat(metadata.getLocale()).isEqualTo("de");
    }

    @Test
    void shouldReturnCorrectEmailTemplate() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder()
            .userName("john.doe")
            .build();

        assertThat(metadata.getEmailTemplate()).isEqualTo(EmailTemplate.WELCOME);
    }

    @Test
    void shouldConvertToMapWithoutLocale() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder()
            .userName("john.doe")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(1);
        assertThat(map.get("userName")).isEqualTo("john.doe");
    }

    @Test
    void shouldConvertToMapWithLocale() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder()
            .userName("john.doe")
            .locale("de")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(2);
        assertThat(map.get("locale")).isEqualTo("de");
    }

    @Test
    void shouldAllowBuildingWithNullFields() {
        final WelcomeMetadata metadata = WelcomeMetadata.builder().build();

        assertThat(metadata.getUserName()).isNull();
    }
}

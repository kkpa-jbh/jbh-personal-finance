package com.jbh.notification.contracts.email.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductVerificationMetadataTest {

  @Test
  void shouldBuildMetadataWithAllRequiredFields() {
    final ProductVerificationMetadata metadata =
        ProductVerificationMetadata.builder()
            .userName("john.doe")
            .verificationCode("123456")
            .build();

    assertThat(metadata.getUserName()).isEqualTo("john.doe");
    assertThat(metadata.getVerificationCode()).isEqualTo("123456");
    assertThat(metadata.getLocale()).isNull();
  }

  @Test
  void shouldBuildMetadataWithOptionalLocale() {
    final ProductVerificationMetadata metadata =
        ProductVerificationMetadata.builder()
            .userName("john.doe")
            .verificationCode("123456")
            .locale("pt")
            .build();

    assertThat(metadata.getLocale()).isEqualTo("pt");
  }

  @Test
  void shouldReturnCorrectEmailTemplate() {
    final ProductVerificationMetadata metadata =
        ProductVerificationMetadata.builder()
            .userName("john.doe")
            .verificationCode("123456")
            .build();

    assertThat(metadata.getEmailTemplate()).isEqualTo(EmailTemplate.ACCOUNT_VERIFICATION);
  }

  @Test
  void shouldConvertToMapWithoutLocale() {
    final ProductVerificationMetadata metadata =
        ProductVerificationMetadata.builder()
            .userName("john.doe")
            .verificationCode("123456")
            .build();

    final Map<String, Object> map = metadata.toMap();

    assertThat(map).hasSize(2);
    assertThat(map.get("userName")).isEqualTo("john.doe");
    assertThat(map.get("verificationCode")).isEqualTo("123456");
  }

  @Test
  void shouldConvertToMapWithLocale() {
    final ProductVerificationMetadata metadata =
        ProductVerificationMetadata.builder()
            .userName("john.doe")
            .verificationCode("123456")
            .locale("pt")
            .build();

    final Map<String, Object> map = metadata.toMap();

    assertThat(map).hasSize(3);
    assertThat(map.get("locale")).isEqualTo("pt");
  }

  @Test
  void shouldAllowBuildingWithNullFields() {
    final ProductVerificationMetadata metadata = ProductVerificationMetadata.builder().build();

    assertThat(metadata.getUserName()).isNull();
    assertThat(metadata.getVerificationCode()).isNull();
  }
}

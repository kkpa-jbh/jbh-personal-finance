package com.jbh.notification.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.templates.TeamInvitationMetadata;
import com.jbh.notification.contracts.email.templates.WelcomeMetadata;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendNotificationRequestTest {

  @Test
  void shouldBuildRequestWithTemplateMetadata() {
    final UUID recipientId = UUID.randomUUID();
    final UUID teamId = UUID.randomUUID();

    final TeamInvitationMetadata metadata =
        TeamInvitationMetadata.builder()
            .teamId(teamId)
            .teamName("Finance Team")
            .inviterName("Jane Doe")
            .locale("es")
            .build();

    final SendNotificationRequest request =
        SendNotificationRequest.builder()
            .recipientId(recipientId)
            .recipientEmail("test@example.com")
            .templateMetadata(metadata)
            .build();

    assertThat(request.recipientId()).isEqualTo(recipientId);
    assertThat(request.recipientEmail()).isEqualTo("test@example.com");
    assertThat(request.emailTemplate()).isEqualTo(EmailTemplate.TEAM_INVITATION);
    assertThat(request.metadata()).containsEntry("teamId", teamId);
    assertThat(request.metadata()).containsEntry("teamName", "Finance Team");
    assertThat(request.metadata()).containsEntry("inviterName", "Jane Doe");
    assertThat(request.metadata()).containsEntry("locale", "es");
  }

  @Test
  void shouldBuildRequestWithWelcomeMetadata() {
    final UUID recipientId = UUID.randomUUID();

    final WelcomeMetadata metadata = WelcomeMetadata.builder().userName("john.doe").build();

    final SendNotificationRequest request =
        SendNotificationRequest.builder()
            .recipientId(recipientId)
            .recipientEmail("john@example.com")
            .templateMetadata(metadata)
            .build();

    assertThat(request.emailTemplate()).isEqualTo(EmailTemplate.WELCOME);
    assertThat(request.metadata()).containsEntry("userName", "john.doe");
    assertThat(request.metadata()).hasSize(1);
  }
}

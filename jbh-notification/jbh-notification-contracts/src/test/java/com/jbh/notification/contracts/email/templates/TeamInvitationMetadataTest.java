package com.jbh.notification.contracts.email.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamInvitationMetadataTest {

    @Test
    void shouldBuildMetadataWithAllRequiredFields() {
        final UUID teamId = UUID.randomUUID();

        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder()
            .teamId(teamId)
            .teamName("Test Team")
            .inviterName("John Doe")
            .build();

        assertThat(metadata.getTeamId()).isEqualTo(teamId);
        assertThat(metadata.getTeamName()).isEqualTo("Test Team");
        assertThat(metadata.getInviterName()).isEqualTo("John Doe");
        assertThat(metadata.getLocale()).isNull();
    }

    @Test
    void shouldBuildMetadataWithOptionalLocale() {
        final UUID teamId = UUID.randomUUID();

        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder()
            .teamId(teamId)
            .teamName("Test Team")
            .inviterName("John Doe")
            .locale("es")
            .build();

        assertThat(metadata.getLocale()).isEqualTo("es");
    }

    @Test
    void shouldReturnCorrectEmailTemplate() {
        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder()
            .teamId(UUID.randomUUID())
            .teamName("Test Team")
            .inviterName("John Doe")
            .build();

        assertThat(metadata.getEmailTemplate()).isEqualTo(EmailTemplate.TEAM_INVITATION);
    }

    @Test
    void shouldConvertToMapWithoutLocale() {
        final UUID teamId = UUID.randomUUID();

        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder()
            .teamId(teamId)
            .teamName("Test Team")
            .inviterName("John Doe")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(3);
        assertThat(map.get("teamId")).isEqualTo(teamId);
        assertThat(map.get("teamName")).isEqualTo("Test Team");
        assertThat(map.get("inviterName")).isEqualTo("John Doe");
    }

    @Test
    void shouldConvertToMapWithLocale() {
        final UUID teamId = UUID.randomUUID();

        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder()
            .teamId(teamId)
            .teamName("Test Team")
            .inviterName("John Doe")
            .locale("es")
            .build();

        final Map<String, Object> map = metadata.toMap();

        assertThat(map).hasSize(4);
        assertThat(map.get("locale")).isEqualTo("es");
    }

    @Test
    void shouldAllowBuildingWithNullFields() {
        final TeamInvitationMetadata metadata = TeamInvitationMetadata.builder().build();

        assertThat(metadata.getTeamId()).isNull();
        assertThat(metadata.getTeamName()).isNull();
        assertThat(metadata.getInviterName()).isNull();
    }
}

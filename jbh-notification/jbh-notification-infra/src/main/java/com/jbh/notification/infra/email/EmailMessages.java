package com.jbh.notification.infra.email;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

/**
 * Type-safe message bundle for email content.
 * Quarkus Qute will automatically load messages from resources/messages/email_{locale}.properties
 */
@MessageBundle(value = "email", locale = "es", defaultKey = Message.HYPHENATED_ELEMENT_NAME)
public interface EmailMessages {

    // Team Invitation
    @Message
    String teamInvitationSubject();

    @Message
    String teamInvitationGreeting();

    @Message
    String teamInvitationIntro();

    @Message
    String teamInvitationBody();

    @Message
    String teamInvitationCtaIntro();

    @Message
    String teamInvitationButton();

    @Message
    String teamInvitationClosing();

    @Message
    String teamInvitationSignature();

    @Message
    String teamInvitationInvitedBy();

    @Message
    String teamInvitationTeamName();

    // Common
    @Message
    String commonFooterText();

    @Message
    String commonUnsubscribe();
}

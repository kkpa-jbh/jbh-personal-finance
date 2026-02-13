package com.jbh.notification.contracts.email;

public enum EmailTemplate {

    TEAM_INVITATION("team-invitation", "email.team-invitation.subject"),
    WELCOME("welcome", "email.welcome.subject"),
    PASSWORD_RESET("password-reset", "email.password-reset.subject"),
    ACCOUNT_VERIFICATION("product-verification", "email.product-verification.subject");

    private final String templateName;
    private final String subjectKey;

    EmailTemplate(final String templateName, final String subjectKey) {
        this.templateName = templateName;
        this.subjectKey = subjectKey;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public String getTemplatePath() {
        return "emails/" + templateName + ".html";
    }
}

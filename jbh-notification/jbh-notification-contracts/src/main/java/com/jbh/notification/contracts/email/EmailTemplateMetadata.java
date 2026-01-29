package com.jbh.notification.contracts.email;

import com.jbh.notification.contracts.common.TemplateMetadata;

public interface EmailTemplateMetadata extends TemplateMetadata {

    EmailTemplate getEmailTemplate();
}

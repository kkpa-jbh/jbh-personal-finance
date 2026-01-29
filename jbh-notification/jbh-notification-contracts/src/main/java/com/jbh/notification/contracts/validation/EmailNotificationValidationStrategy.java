package com.jbh.notification.contracts.validation;

import com.jbh.notification.contracts.SendNotificationRequest;
import com.jbh.notification.contracts.email.EmailMetadataKey;
import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EmailNotificationValidationStrategy extends BaseNotificationValidationStrategy {

  private static final Map<EmailTemplate, Set<EmailMetadataKey>> REQUIRED_KEYS;
  private static final Map<EmailTemplate, Set<EmailMetadataKey>> OPTIONAL_KEYS;

  static {
    final Map<EmailTemplate, Set<EmailMetadataKey>> required = new EnumMap<>(EmailTemplate.class);
    required.put(
        EmailTemplate.TEAM_INVITATION,
        EnumSet.of(
            EmailMetadataKey.TEAM_ID, EmailMetadataKey.TEAM_NAME, EmailMetadataKey.INVITER_NAME));
    required.put(
        EmailTemplate.PASSWORD_RESET,
        EnumSet.of(EmailMetadataKey.USER_NAME, EmailMetadataKey.RESET_TOKEN));
    required.put(EmailTemplate.WELCOME, EnumSet.of(EmailMetadataKey.USER_NAME));
    required.put(
        EmailTemplate.ACCOUNT_VERIFICATION,
        EnumSet.of(EmailMetadataKey.USER_NAME, EmailMetadataKey.VERIFICATION_CODE));
    REQUIRED_KEYS = Collections.unmodifiableMap(required);

    final Map<EmailTemplate, Set<EmailMetadataKey>> optional = new EnumMap<>(EmailTemplate.class);
    optional.put(EmailTemplate.TEAM_INVITATION, EnumSet.of(EmailMetadataKey.LOCALE));
    optional.put(EmailTemplate.PASSWORD_RESET, EnumSet.of(EmailMetadataKey.LOCALE));
    optional.put(EmailTemplate.WELCOME, EnumSet.of(EmailMetadataKey.LOCALE));
    optional.put(EmailTemplate.ACCOUNT_VERIFICATION, EnumSet.of(EmailMetadataKey.LOCALE));
    OPTIONAL_KEYS = Collections.unmodifiableMap(optional);
  }

  @Override
  public List<ValidationError> validate(final SendNotificationRequest request) {
    final List<ValidationError> errors = new ArrayList<>(validateCommonFields(request));

    final EmailTemplate template = request.emailTemplate();

    if (template != null) {
      errors.addAll(validateTemplateEmail(request, template));
    } else {
      errors.addAll(validateNonTemplateEmail(request));
    }

    return errors;
  }

  private List<ValidationError> validateTemplateEmail(
      final SendNotificationRequest request, final EmailTemplate template) {
    return collectErrors(template, request.metadata());
  }

  private List<ValidationError> validateNonTemplateEmail(final SendNotificationRequest request) {
    final List<ValidationError> errors = new ArrayList<>();

    if (isBlank(request.subject())) {
      errors.add(ValidationError.required("subject"));
    }

    if (isBlank(request.message())) {
      errors.add(ValidationError.required("message"));
    }

    return errors;
  }

  public List<ValidationError> collectErrors(
      final EmailTemplate template, final Map<String, Object> metadata) {
    final Set<EmailMetadataKey> requiredKeys = getRequiredKeys(template);
    final Map<String, Object> safeMetadata = metadata != null ? metadata : Collections.emptyMap();
    final List<ValidationError> errors = new ArrayList<>();

    for (final EmailMetadataKey key : requiredKeys) {
      final String keyName = key.getKey();
      final Object value = safeMetadata.get(keyName);

      if (value == null) {
        errors.add(ValidationError.missingMetadataKey(keyName, template.name()));
      } else if (value instanceof String && ((String) value).isBlank()) {
        errors.add(ValidationError.blank("metadata." + keyName, value));
      }
    }

    return errors;
  }

  private Set<EmailMetadataKey> getRequiredKeys(final EmailTemplate template) {
    return REQUIRED_KEYS.getOrDefault(template, Collections.emptySet());
  }
}

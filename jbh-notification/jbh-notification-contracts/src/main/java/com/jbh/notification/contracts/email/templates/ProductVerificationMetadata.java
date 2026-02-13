package com.jbh.notification.contracts.email.templates;

import com.jbh.notification.contracts.email.EmailMetadataKey;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.EmailTemplateMetadata;
import java.util.HashMap;
import java.util.Map;

public final class ProductVerificationMetadata implements EmailTemplateMetadata {

  private final String userName;
  private final String verificationCode;
  private final String locale;

  private ProductVerificationMetadata(final Builder builder) {
    this.userName = builder.userName;
    this.verificationCode = builder.verificationCode;
    this.locale = builder.locale;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public EmailTemplate getEmailTemplate() {
    return EmailTemplate.ACCOUNT_VERIFICATION;
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();
    map.put(EmailMetadataKey.USER_NAME.getKey(), userName);
    map.put(EmailMetadataKey.VERIFICATION_CODE.getKey(), verificationCode);
    if (locale != null) {
      map.put(EmailMetadataKey.LOCALE.getKey(), locale);
    }
    return map;
  }

  public String getUserName() {
    return userName;
  }

  public String getVerificationCode() {
    return verificationCode;
  }

  public String getLocale() {
    return locale;
  }

  public static final class Builder {
    private String userName;
    private String verificationCode;
    private String locale;

    private Builder() {}

    public Builder userName(final String userName) {
      this.userName = userName;
      return this;
    }

    public Builder verificationCode(final String verificationCode) {
      this.verificationCode = verificationCode;
      return this;
    }

    public Builder locale(final String locale) {
      this.locale = locale;
      return this;
    }

    public ProductVerificationMetadata build() {
      return new ProductVerificationMetadata(this);
    }
  }
}

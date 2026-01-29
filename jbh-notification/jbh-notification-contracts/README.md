# Future Extensibility Example

When adding SMS support:

```
sms/                                                                                                                                                                                                                          
├── SmsTemplate.java                    # enum SMS_VERIFICATION, SMS_ALERT, etc.                                                                                                                                              
├── SmsMetadataKey.java                 # enum PHONE_NUMBER, MESSAGE_TEXT, etc.                                                                                                                                               
├── SmsTemplateMetadata.java            # interface extends TemplateMetadata                                                                                                                                                  
├── SmsTemplateRequirements.java        # utility class                                                                                                                                                                       
└── templates/                                                                                                                                                                                                                
├── SmsVerificationMetadata.java                                                                                                                                                                                          
└── SmsAlertMetadata.java

validation/                                                                                                                                                                                                                   
└── SmsNotificationValidationStrategy.java  # New strategy for SMS validation   
```
package com.khoi.ewallet.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionMailConfigurationValidator {
    private final String smtpPassword;
    private final String fromAddress;

    public ProductionMailConfigurationValidator(
            @Value("${spring.mail.password:}") String smtpPassword,
            @Value("${mail.from-address:}") String fromAddress) {
        this.smtpPassword = smtpPassword;
        this.fromAddress = fromAddress;
    }

    @PostConstruct
    void validate() {
        requireValue(smtpPassword, "SMTP_PASSWORD");
        requireValue(fromAddress, "MAIL_FROM_ADDRESS");
    }

    private void requireValue(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable + " environment variable is required for the prod profile");
        }
    }
}

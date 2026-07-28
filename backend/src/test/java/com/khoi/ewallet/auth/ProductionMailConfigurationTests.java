package com.khoi.ewallet.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionMailConfigurationTests {

    @Test
    void productionProfileUsesProviderNeutralResendSmtpDefaults() throws IOException {
        Properties properties = loadProductionProperties();

        assertEquals("${SMTP_HOST:smtp.resend.com}", properties.getProperty("spring.mail.host"));
        assertEquals("${SMTP_PORT:587}", properties.getProperty("spring.mail.port"));
        assertEquals("${SMTP_USERNAME:resend}", properties.getProperty("spring.mail.username"));
        assertEquals("${SMTP_PASSWORD}", properties.getProperty("spring.mail.password"));
        assertEquals("${MAIL_FROM_ADDRESS}", properties.getProperty("mail.from-address"));
        assertEquals("true", properties.getProperty("spring.mail.properties.mail.smtp.auth"));
        assertEquals("true", properties.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        assertEquals("true", properties.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
    }

    @Test
    void productionProfileDoesNotReferenceLegacySesVariables() throws IOException {
        try (InputStream stream = productionPropertiesStream()) {
            String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(contents.contains("SES_SMTP_"));
        }
    }

    @Test
    void productionValidationRequiresSmtpPassword() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new ProductionMailConfigurationValidator("", "sender@example.test").validate());

        assertTrue(exception.getMessage().contains("SMTP_PASSWORD"));
    }

    @Test
    void productionValidationRequiresFromAddress() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new ProductionMailConfigurationValidator("test-only-password", " ").validate());

        assertTrue(exception.getMessage().contains("MAIL_FROM_ADDRESS"));
    }

    @Test
    void productionValidationAcceptsExternallySuppliedValues() {
        new ProductionMailConfigurationValidator(
                "test-only-password", "sender@example.test").validate();
    }

    private Properties loadProductionProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = productionPropertiesStream()) {
            properties.load(stream);
        }
        return properties;
    }

    private InputStream productionPropertiesStream() {
        InputStream stream = getClass().getResourceAsStream("/application-prod.properties");
        if (stream == null) {
            throw new IllegalStateException("application-prod.properties is missing");
        }
        return stream;
    }
}

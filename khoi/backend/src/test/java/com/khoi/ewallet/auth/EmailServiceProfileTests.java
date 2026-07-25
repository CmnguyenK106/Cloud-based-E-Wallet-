package com.khoi.ewallet.auth;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class EmailServiceProfileTests {

    @Test
    void localProfileSelectsOnlyDevelopmentEmailService() {
        try (AnnotationConfigApplicationContext context = emailContext("local")) {
            assertEquals(1, context.getBeansOfType(EmailService.class).size());
            assertInstanceOf(DevelopmentEmailService.class, context.getBean(EmailService.class));
            assertFalse(context.containsBean("smtpEmailService"));
        }
    }

    @Test
    void productionProfileSelectsOnlySmtpEmailService() {
        try (AnnotationConfigApplicationContext context = emailContext("prod")) {
            assertEquals(1, context.getBeansOfType(EmailService.class).size());
            assertInstanceOf(SmtpEmailService.class, context.getBean(EmailService.class));
        }
    }

    private AnnotationConfigApplicationContext emailContext(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("testMail", Map.of("mail.from-address", "sender@example.test")));
        context.registerBean(JavaMailSender.class, () -> mock(JavaMailSender.class));
        context.register(DevelopmentEmailService.class, SmtpEmailService.class);
        context.refresh();
        return context;
    }
}

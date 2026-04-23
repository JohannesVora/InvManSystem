package com.invman.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.example.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSenderImpl javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        applySmtpProperties(sender.getJavaMailProperties(), port);
        return sender;
    }

    static void applySmtpProperties(Properties props, int port) {
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (port == 465) {
            // SMTPS: SSL from the start
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            // STARTTLS (587 or custom)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.enable", "false");
        }
        props.put("mail.debug", "false");
    }
}

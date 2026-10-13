package com.manhnv.capstonebakerai.identity.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailTemplateService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    private final JavaMailSender mailSender;
    private final String sender;

    public EmailTemplateService(JavaMailSender mailSender, @Value("${app.mail.from}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    public void send(String to, String subject, String template, String plainText, Map<String, String> values) {
        try {
            String html = new ClassPathResource("templates/email/" + template)
                    .getContentAsString(StandardCharsets.UTF_8);
            Matcher matcher = PLACEHOLDER.matcher(html);
            StringBuilder rendered = new StringBuilder();
            while (matcher.find()) {
                String value = values.getOrDefault(matcher.group(1), "");
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(escapeHtml(value)));
            }
            matcher.appendTail(rendered);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(sender);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText, rendered.toString());
            mailSender.send(message);
        } catch (IOException | MessagingException exception) {
            throw new MailPreparationException("Could not prepare email from template " + template, exception);
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

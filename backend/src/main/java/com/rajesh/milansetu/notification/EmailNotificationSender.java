package com.rajesh.milansetu.notification;

import com.rajesh.milansetu.config.AppProperties;
import com.rajesh.milansetu.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Real e-mail through Spring Mail (any SMTP server; locally Mailpit). No paid SMS gateway needed. */
@Component
@ConditionalOnProperty(prefix = "app.notifications", name = "mode", havingValue = "email")
public class EmailNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);
    private final JavaMailSender mail;
    private final String from;

    public EmailNotificationSender(JavaMailSender mail, AppProperties props) {
        this.mail = mail;
        this.from = props.notifications().fromAddress();
    }

    @Override
    public void send(User to, String subject, String body) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to.getEmail());
        m.setSubject(subject);
        m.setText(body + "\n\n-- MilanSetu (portfolio project, demonstration only)");
        mail.send(m);
        log.info("Email '{}' sent to user {}", subject, to.getId());
    }

    @Override
    public void sendOtp(User guardian, Long caseId, String code, LocalDateTime expiresAt) {
        send(guardian, "MilanSetu handover OTP (case " + caseId + ")",
                "Your one-time handover code is " + code + ".\nShow it ONLY to the help desk staff when you collect your family member."
                        + "\nIt is valid until " + expiresAt + " and works once.");
    }

    @Override
    public String channelName() { return "EMAIL"; }
}

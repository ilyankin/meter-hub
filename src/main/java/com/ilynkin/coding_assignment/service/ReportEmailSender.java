package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.config.AppProperties;
import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class ReportEmailSender {

    private final JavaMailSender mailSender;
    private final String recipient;
    private final String from;
    private final String subject;

    public ReportEmailSender(JavaMailSender mailSender, AppProperties appProperties) {
        AppProperties.Report report = appProperties.report();
        this.mailSender = mailSender;
        this.recipient = report.recipient();
        this.from = report.from();
        this.subject = report.subject();
    }

    public void send(ReportFile report) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body(report.periodDays()));
            helper.addAttachment(report.filename(), new ByteArrayResource(report.content()), "text/csv");
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to collect report letter", ex);
        }
        mailSender.send(message);
        log.info("Отчёт '{}' отправлен на {}", report.filename(), recipient);
    }

    private static String body(int periodDays) {
        return "Во вложении CSV с последними показаниями всех приборов учёта за последние "
                + periodDays + " дн.";
    }
}

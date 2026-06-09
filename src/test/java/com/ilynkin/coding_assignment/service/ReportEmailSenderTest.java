package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportEmailSenderTest {

    @Test
    void send_buildsMessageWithCsvAttachment() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        ReportEmailSender sender = new ReportEmailSender(
                mailSender, "user@example.com", "noreply@meterhub.local", "Показания");

        sender.send(new ReportFile("meter-readings-2024-02-20.csv",
                "serial,date,T1\nSN-1,2024-02-20,100\n".getBytes(StandardCharsets.UTF_8), 14));

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()[0]).hasToString("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Показания");

        jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) message.getContent();
        String body = multipart.getBodyPart(0).getContent().toString();
        assertThat(body).contains("14 дн.");
    }
}

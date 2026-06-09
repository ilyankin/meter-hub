package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.entity.Meter;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MeterReadingReportServiceTest {
    private static final String MANAGER_EMAIL = "manager@example.com";

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private MeterReadingReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeterRepository meterRepository;

    private String serial;

    @BeforeEach
    void setUp() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        serial = "SN-REPORT-" + System.nanoTime();
        meterRepository.save(Meter.builder()
                .user(manager)
                .serialNumber(serial)
                .build());
    }

    private MultipartFile csv(String content) {
        return new MockMultipartFile("file", "readings.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void report_containsOnlyLatestReadingInWindowPerMeter() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String recent = today.minusDays(2).toString();
        String earlier = today.minusDays(5).toString();
        String far = today.minusDays(40).toString();

        csvImportService.importReadings(csv("""
                serial,date,T1,T2
                %s,%s,50,10
                %s,%s,100.5,50.25
                %s,%s,200,75
                """.formatted(serial, far, serial, earlier, serial, recent)));

        ReportFile report = reportService.buildLatestReadingsReport();
        String content = new String(report.content(), StandardCharsets.UTF_8);

        assertThat(report.filename()).startsWith("meter-readings-").endsWith(".csv");
        assertThat(report.periodDays()).isEqualTo(14);

        List<String> ourLines = content.lines().filter(l -> l.contains(serial)).toList();
        assertThat(ourLines).hasSize(1);
        assertThat(ourLines.getFirst())
                .contains(recent)
                .contains("200")
                .doesNotContain(earlier)
                .doesNotContain(far);
    }

    @Test
    void report_hasImportCompatibleHeader() {
        ReportFile report = reportService.buildLatestReadingsReport();
        String header = new String(report.content(), StandardCharsets.UTF_8).lines().findFirst().orElseThrow();

        assertThat(header).startsWith("serial,date");
    }
}

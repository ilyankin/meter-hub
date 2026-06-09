package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.response.CsvImportResponse;
import com.ilynkin.coding_assignment.entity.Meter;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.exception.CsvImportException;
import com.ilynkin.coding_assignment.repository.MeterReadingRepository;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CsvImportServiceTest {

    private static final String MANAGER_EMAIL = "manager@example.com";

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeterRepository meterRepository;

    @Autowired
    private MeterReadingRepository readingRepository;

    private String serial;

    @BeforeEach
    void setUp() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        serial = "SN-CSV-" + System.nanoTime();
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
    void validCsv_createsReadings() {
        long before = readingRepository.count();

        CsvImportResponse result = csvImportService.importReadings(csv("""
                serial,date,T1,T2
                %s,2024-01-15,100.5,50.25
                %s,2024-02-15,10,
                """.formatted(serial, serial)));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(readingRepository.count()).isEqualTo(before + 2);
    }

    @Test
    void unknownSerial_throws_andSavesNothing() {
        long before = readingRepository.count();

        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T1
                NOPE-404,2024-01-15,5
                """)))
                .isInstanceOf(CsvImportException.class);

        assertThat(readingRepository.count()).isEqualTo(before);
    }

    @Test
    void unknownZoneColumn_throws() {
        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T9
                %s,2024-01-15,5
                """.formatted(serial))))
                .isInstanceOf(CsvImportException.class)
                .satisfies(ex -> assertThat(((CsvImportException) ex).getErrors())
                        .anySatisfy(e -> assertThat(e.column()).isEqualTo("T9")));
    }

    @Test
    void rowWithNoValues_throws() {
        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T1,T2
                %s,2024-01-15,,
                """.formatted(serial))))
                .isInstanceOf(CsvImportException.class);
    }

    @Test
    void inFileDuplicate_throws() {
        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T1
                %s,2024-01-15,5
                %s,2024-01-15,6
                """.formatted(serial, serial))))
                .isInstanceOf(CsvImportException.class);
    }

    @Test
    void dbDuplicate_throws() {
        csvImportService.importReadings(csv("""
                serial,date,T1
                %s,2024-03-15,5
                """.formatted(serial)));

        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T1
                %s,2024-03-15,7
                """.formatted(serial))))
                .isInstanceOf(CsvImportException.class);
    }

    @Test
    void notANumber_throws() {
        assertThatThrownBy(() -> csvImportService.importReadings(csv("""
                serial,date,T1
                %s,2024-01-15,abc
                """.formatted(serial))))
                .isInstanceOf(CsvImportException.class);
    }
}

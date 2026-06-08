package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.request.MeterReadingRequest;
import com.ilynkin.coding_assignment.dto.request.MeterReadingValueRequest;
import com.ilynkin.coding_assignment.dto.response.MeterReadingResponse;
import com.ilynkin.coding_assignment.dto.response.MeterReadingValueResponse;
import com.ilynkin.coding_assignment.entity.Meter;
import com.ilynkin.coding_assignment.entity.MeterReading;
import com.ilynkin.coding_assignment.entity.MeterReadingValue;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
class MeterReadingServiceTest {

    private static final String MANAGER_EMAIL = "manager@example.com";

    @Autowired
    private MeterReadingService readingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeterRepository meterRepository;

    private Long meterId;

    @BeforeEach
    void setUp() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        Meter meter = meterRepository.save(Meter.builder()
                .user(manager)
                .serialNumber("SN-SVC-" + System.nanoTime())
                .build());
        meterId = meter.getId();
    }

    @Test
    void update_replacesValues_andRemovesOrphans() {
        UUID id = readingService.create(request(Instant.parse("2024-01-15T00:00:00Z"),
                value("T1", "100.5"), value("T2", "50.25"))).id();

        readingService.update(id, request(Instant.parse("2024-02-01T00:00:00Z"),
                value("T1", "10")));

        MeterReadingResponse reloaded = readingService.findById(id);
        assertThat(reloaded.values())
                .extracting(MeterReadingValueResponse::tariffZone)
                .containsExactly("T1");
        assertThat(reloaded.values().getFirst().value()).isEqualByComparingTo("10.000");
    }

    @Test
    void update_keepsZone_andAddsNewZone() {
        UUID id = readingService.create(request(null, value("T1", "1.0"))).id();

        readingService.update(id, request(null, value("T1", "5"), value("T2", "7")));

        MeterReadingResponse reloaded = readingService.findById(id);
        assertThat(reloaded.values())
                .extracting(MeterReadingValueResponse::tariffZone, MeterReadingValueResponse::value)
                .containsExactlyInAnyOrder(
                        tuple("T1", new BigDecimal("5.000")),
                        tuple("T2", new BigDecimal("7.000")));
    }

    @Test
    void create_defaultsReadingDate_toNow_whenNull() {
        MeterReadingResponse created = readingService.create(request(null, value("T1", "1.0")));

        assertThat(created.readingDate())
                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
    }

    @Test
    void create_unknownMeter_throwsNotFound() {
        MeterReadingRequest request = new MeterReadingRequest(999_999L, null, List.of(value("T1", "1.0")));

        assertThatThrownBy(() -> readingService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meter not found");
    }

    @Test
    void create_unknownTariffZone_throwsNotFound() {
        assertThatThrownBy(() -> readingService.create(request(null, value("T9", "1.0"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("T9");
    }

    @Test
    void meterReading_getValues_isUnmodifiable() {
        MeterReading reading = MeterReading.builder().build();
        reading.replaceValues(List.of(MeterReadingValue.builder().build()));

        assertThat(reading.getValues()).hasSize(1);
        assertThatThrownBy(() -> reading.getValues().add(MeterReadingValue.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private MeterReadingRequest request(Instant readingDate, MeterReadingValueRequest... values) {
        return new MeterReadingRequest(meterId, readingDate, List.of(values));
    }

    private static MeterReadingValueRequest value(String zone, String value) {
        return new MeterReadingValueRequest(zone, new BigDecimal(value));
    }
}

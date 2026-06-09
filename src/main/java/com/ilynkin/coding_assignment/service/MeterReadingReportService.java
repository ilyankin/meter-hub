package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.entity.MeterReading;
import com.ilynkin.coding_assignment.entity.MeterReadingValue;
import com.ilynkin.coding_assignment.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeterReadingReportService {
    private static final String COL_SERIAL = "serial";
    private static final String COL_DATE = "date";

    private final MeterReadingRepository readingRepository;

    @Value("${app.report.period-days}")
    private int periodDays;

    public record ReportFile(String filename, byte[] content, int periodDays) {
    }

    @Transactional(readOnly = true)
    public ReportFile buildLatestReadingsReport() {
        Instant since = Instant.now().minus(Duration.ofDays(periodDays));
        List<MeterReading> readings = readingRepository.findLatestPerMeterSince(since);

        TreeSet<String> zoneCodes = readings.stream()
                .flatMap(r -> r.getValues().stream())
                .map(v -> v.getTariffZone().getCode())
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> header = new ArrayList<>(List.of(COL_SERIAL, COL_DATE));
        header.addAll(zoneCodes);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(header.toArray(String[]::new))
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), format)) {
            for (MeterReading reading : readings) {
                Map<String, BigDecimal> byCode = reading.getValues().stream()
                        .collect(Collectors.toMap(
                                v -> v.getTariffZone().getCode(),
                                MeterReadingValue::getReadingValue)
                        );

                List<String> record = new ArrayList<>();
                record.add(reading.getMeter().getSerialNumber());
                record.add(reading.getReadingDate().atOffset(ZoneOffset.UTC).toLocalDate().toString());
                for (String code : zoneCodes) {
                    BigDecimal value = byCode.get(code);
                    record.add(value == null ? "" : value.toPlainString());
                }
                printer.printRecord(record);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate CSV report", ex);
        }

        String filename = "meter-readings-" + periodDays + "-days" + LocalDate.now(ZoneOffset.UTC) + ".csv";
        return new ReportFile(filename, out.toByteArray(), periodDays);
    }
}

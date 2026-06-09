package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.response.CsvImportResponse;
import com.ilynkin.coding_assignment.entity.*;
import com.ilynkin.coding_assignment.exception.CsvImportException;
import com.ilynkin.coding_assignment.exception.CsvImportException.RowError;
import com.ilynkin.coding_assignment.repository.MeterReadingRepository;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.TariffZoneRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final String COL_SERIAL = "serial";
    private static final String COL_DATE = "date";

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .setTrim(true)
            .build();

    private final MeterRepository meterRepository;
    private final TariffZoneRepository tariffZoneRepository;
    private final MeterReadingRepository readingRepository;

    @Transactional
    public CsvImportResponse importReadings(MultipartFile file) {
        return new ImportRunner().run(file);
    }

    private final class ImportRunner {
        private record ParsedRow(long line, String serial, Instant readingDate, Map<String, BigDecimal> valuesByZone) {
        }

        private record SerialDate(String serial, Instant readingDate) {
        }

        private record MeterDate(long meterId, Instant readingDate) {
        }


        private final List<RowError> errors = new ArrayList<>();
        private final List<ParsedRow> rows = new ArrayList<>();

        private String serialHeader;
        private String dateHeader;
        private List<String> zoneHeaders;

        private Map<String, TariffZone> zonesByCode;
        private Map<String, Meter> metersBySerial;

        CsvImportResponse run(MultipartFile file) {
            parse(file);

            if (rows.isEmpty() && errors.isEmpty()) {
                throw new CsvImportException(List.of(new RowError(0, "file", "File contains no data rows")));
            }

            Set<String> serials = rows.stream().map(ParsedRow::serial).collect(Collectors.toSet());
            metersBySerial = meterRepository.findBySerialNumberIn(serials)
                    .stream()
                    .collect(Collectors.toMap(Meter::getSerialNumber, Function.identity()));

            validateWithDb();

            if (!errors.isEmpty()) {
                throw new CsvImportException(errors);
            }

            return persist();
        }

        private void parse(MultipartFile file) {
            try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                 CSVParser parser = CSV_FORMAT.parse(reader)) {

                resolveHeader(parser);
                for (CSVRecord rec : parser) {
                    parseRow(rec);
                }
            } catch (IOException | IllegalArgumentException ex) {
                throw new CsvImportException(List.of(new RowError(0, "file",
                        "Failed to read CSV: " + ex.getMessage())));
            }
        }

        private void resolveHeader(CSVParser parser) {
            // serial/date зарезервированы (case-insensitive), остальное - коды зон
            Map<String, String> byLower = new LinkedHashMap<>();
            for (String headerName : parser.getHeaderNames()) {
                byLower.put(headerName.toLowerCase(), headerName);
            }
            serialHeader = byLower.get(COL_SERIAL);
            dateHeader = byLower.get(COL_DATE);
            if (serialHeader == null || dateHeader == null) {
                throw new CsvImportException(List.of(new RowError(0, "header",
                        "Header must contain 'serial' and 'date' columns")));
            }
            zoneHeaders = parser.getHeaderNames()
                    .stream()
                    .filter(h -> !h.equalsIgnoreCase(COL_SERIAL) && !h.equalsIgnoreCase(COL_DATE))
                    .toList();

            zonesByCode = tariffZoneRepository.findByCodeIn(zoneHeaders)
                    .stream()
                    .collect(Collectors.toMap(TariffZone::getCode, Function.identity()));
            for (String zh : zoneHeaders) {
                if (!zonesByCode.containsKey(zh)) {
                    errors.add(new RowError(0, zh, "Unknown tariff zone"));
                }
            }
        }

        private void parseRow(CSVRecord rec) {
            long line = rec.getRecordNumber();
            String serial = cell(rec, serialHeader);
            if (!StringUtils.hasText(serial)) {
                errors.add(new RowError(line, COL_SERIAL, "Empty serial number"));
            }

            Instant readingDate = parseReadingDate(line, cell(rec, dateHeader));
            Map<String, BigDecimal> valuesByZone = parseValues(rec, line);

            if (StringUtils.hasText(serial) && readingDate != null) {
                rows.add(new ParsedRow(line, serial, readingDate, valuesByZone));
            }
        }

        private Instant parseReadingDate(long line, String dateStr) {
            if (!StringUtils.hasText(dateStr)) {
                errors.add(new RowError(line, COL_DATE, "Empty date"));
                return null;
            }
            try {
                return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ex) {
                errors.add(new RowError(line, COL_DATE, "Date is not in yyyy-MM-dd format: " + dateStr));
                return null;
            }
        }

        private Map<String, BigDecimal> parseValues(CSVRecord rec, long line) {
            Map<String, BigDecimal> valuesByZone = new LinkedHashMap<>();
            for (String zoneHeader : zoneHeaders) {
                String raw = cell(rec, zoneHeader);
                if (!StringUtils.hasText(raw) || !zonesByCode.containsKey(zoneHeader)) continue;
                try {
                    BigDecimal value = new BigDecimal(raw);
                    if (value.signum() < 0) {
                        errors.add(new RowError(line, zoneHeader, "Negative value: " + raw));
                    } else {
                        valuesByZone.put(zoneHeader, value);
                    }
                } catch (NumberFormatException ex) {
                    errors.add(new RowError(line, zoneHeader, "Not a number: " + raw));
                }
            }
            if (valuesByZone.isEmpty()) {
                errors.add(new RowError(line, "values", "Row has no tariff-zone values"));
            }
            return valuesByZone;
        }

        private void validateWithDb() {
            if (rows.isEmpty()) {
                return;
            }

            // Неизвестные приборы
            for (ParsedRow row : rows) {
                if (!metersBySerial.containsKey(row.serial())) {
                    errors.add(new RowError(row.line(), COL_SERIAL, "Unknown meter: " + row.serial()));
                }
            }

            // Дубли serial, date внутри файла
            Set<SerialDate> seen = new HashSet<>();
            for (ParsedRow row : rows) {
                if (!seen.add(new SerialDate(row.serial(), row.readingDate()))) {
                    errors.add(new RowError(row.line(), "serial+date", "Duplicate (serial+date) within file"));
                }
            }

            // Дубли с уже существующими показаниями в БД
            List<Long> meterIds = metersBySerial.values().stream().map(Meter::getId).toList();
            List<Instant> dates = rows.stream().map(ParsedRow::readingDate).distinct().toList();
            if (!meterIds.isEmpty()) {
                Set<MeterDate> existingPairs = readingRepository
                        .findByMeterIdInAndReadingDateIn(meterIds, dates)
                        .stream()
                        .map(r -> new MeterDate(r.getMeter().getId(), r.getReadingDate()))
                        .collect(Collectors.toSet());
                for (ParsedRow row : rows) {
                    Meter meter = metersBySerial.get(row.serial());
                    if (meter != null && existingPairs.contains(new MeterDate(meter.getId(), row.readingDate()))) {
                        errors.add(new RowError(row.line(), "serial+date", "Reading already exists in the database"));
                    }
                }
            }
        }

        private CsvImportResponse persist() {
            List<MeterReading> meterReadings = rows
                    .stream()
                    .map(row -> {
                        MeterReading reading = MeterReading.builder()
                                .meter(metersBySerial.get(row.serial()))
                                .readingDate(row.readingDate())
                                .build();

                        List<MeterReadingValue> values = row.valuesByZone().entrySet()
                                .stream()
                                .map(e -> MeterReadingValue.builder()
                                        .id(new MeterReadingValueId())
                                        .reading(reading)
                                        .tariffZone(zonesByCode.get(e.getKey()))
                                        .readingValue(e.getValue())
                                        .build())
                                .toList();
                        reading.replaceValues(values);
                        return reading;
                    }).toList();

            readingRepository.saveAll(meterReadings);
            return new CsvImportResponse(meterReadings.size());
        }
    }
    // Для безопасного чтения ячеек. Короткая строка (колонок меньше, чем в заголовке), чтобы возвращала null,
    // а не IllegalArgumentException
    private static String cell(CSVRecord rec, String column) {
        return rec.isSet(column) ? rec.get(column) : null;
    }
}
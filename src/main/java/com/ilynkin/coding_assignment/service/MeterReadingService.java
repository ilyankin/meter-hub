package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.request.MeterReadingRequest;
import com.ilynkin.coding_assignment.dto.request.MeterReadingValueRequest;
import com.ilynkin.coding_assignment.dto.response.MeterReadingResponse;
import com.ilynkin.coding_assignment.entity.*;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.mapper.MeterReadingMapper;
import com.ilynkin.coding_assignment.repository.MeterReadingRepository;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeterReadingService {
    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final TariffZoneService tariffZoneService;
    private final MeterReadingMapper meterReadingMapper;

    @Transactional(readOnly = true)
    public MeterReadingResponse findById(UUID id) {
        return meterReadingRepository.findById(id)
                .map(meterReadingMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found"));
    }

    @Transactional(readOnly = true)
    public Page<MeterReadingResponse> findAll(Long meterId, Pageable pageable) {
        Page<MeterReading> readings = (meterId == null)
                ? meterReadingRepository.findAll(pageable)
                : meterReadingRepository.findByMeterId(meterId, pageable);
        return readings.map(meterReadingMapper::toResponse);
    }

    @Transactional
    public MeterReadingResponse create(MeterReadingRequest request) {
        MeterReading reading = MeterReading.builder()
                .meter(getMeter(request.meterId()))
                .readingDate(request.readingDate() != null ? request.readingDate() : Instant.now())
                .build();
        reading.replaceValues(buildValues(reading, request.values()));

        return meterReadingMapper.toResponse(meterReadingRepository.saveAndFlush(reading));
    }

    @Transactional
    public MeterReadingResponse update(UUID id, MeterReadingRequest request) {
        MeterReading reading = meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found"));

        reading.setMeter(getMeter(request.meterId()));
        if (request.readingDate() != null) {
            reading.setReadingDate(request.readingDate());
        }
        // Удаление прежние значения отдельным flush'ем до вставки новых, а иначе у сохранившейся
        // зоны новый MeterReadingValue получит тот же составной pk ключ (@MapsId), что и удаляемый
        // сирота — Hibernate в одном flush не может delete+insert один и тот же pk. Перейти на суррогатный
        reading.replaceValues(List.of());
        meterReadingRepository.flush();
        reading.replaceValues(buildValues(reading, request.values()));

        return meterReadingMapper.toResponse(meterReadingRepository.save(reading));
    }

    @Transactional
    public void delete(UUID id) {
        MeterReading reading = meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reading not found"));
        meterReadingRepository.delete(reading);
    }

    private Meter getMeter(Long meterId) {
        return meterRepository.findById(meterId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
    }

    private List<MeterReadingValue> buildValues(MeterReading reading, List<MeterReadingValueRequest> values) {
        Map<String, TariffZone> zonesByCode = tariffZoneService.zonesByCode();

        return values
                .stream()
                .map(v -> {
                    TariffZone zone = zonesByCode.get(v.tariffZone());
                    if (zone == null) {
                        throw new ResourceNotFoundException("Tariff zone " + v.tariffZone() + " not found");
                    }
                    return MeterReadingValue.builder()
                            .id(new MeterReadingValueId()) // Заполняется Hibernate автоматически
                            .reading(reading)
                            .tariffZone(zone)
                            .readingValue(v.value())
                            .build();
                })
                .toList();
    }
}

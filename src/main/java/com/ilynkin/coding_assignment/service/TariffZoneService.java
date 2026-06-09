package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.entity.TariffZone;
import com.ilynkin.coding_assignment.repository.TariffZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TariffZoneService {
    public static final String CACHE_TARIFF_ZONES = "tariffZones";

    private final TariffZoneRepository tariffZoneRepository;

    @Cacheable(CACHE_TARIFF_ZONES)
    public Map<String, TariffZone> zonesByCode() {
        return tariffZoneRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(TariffZone::getCode, Function.identity()));
    }
}

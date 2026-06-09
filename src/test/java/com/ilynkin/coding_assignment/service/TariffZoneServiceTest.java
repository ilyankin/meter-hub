package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.entity.TariffZone;
import com.ilynkin.coding_assignment.repository.TariffZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class TariffZoneServiceTest {

    @Autowired
    private TariffZoneService tariffZoneService;

    @MockitoSpyBean
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetCache() {
        Objects.requireNonNull(cacheManager.getCache(TariffZoneService.CACHE_TARIFF_ZONES)).clear();
        clearInvocations(tariffZoneRepository);
    }

    @Test
    void zonesByCode_hitsDatabaseOnlyOnce() {
        Map<String, TariffZone> first = tariffZoneService.zonesByCode();
        Map<String, TariffZone> second = tariffZoneService.zonesByCode();

        assertThat(first).containsKeys("T1", "T2");
        assertThat(second).containsKeys("T1", "T2");
        verify(tariffZoneRepository, times(1)).findAll();
    }
}

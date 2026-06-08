package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.TariffZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TariffZoneRepository extends JpaRepository<TariffZone, Long> {

    List<TariffZone> findByCodeIn(Collection<String> codes);
}

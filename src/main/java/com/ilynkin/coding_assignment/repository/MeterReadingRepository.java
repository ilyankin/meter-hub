package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);
}

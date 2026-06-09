package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);

    List<MeterReading> findByMeterIdInAndReadingDateIn(Collection<Long> meterIds, Collection<Instant> readingDates);

    @Query("""
            select distinct r from MeterReading r
            join fetch r.meter m
            join fetch r.values v
            join fetch v.tariffZone
            where r.readingDate >= :since
              and r.readingDate = (
                select max(r2.readingDate) from MeterReading r2
                where r2.meter = m and r2.readingDate >= :since
              )
            order by m.serialNumber
            """)
    List<MeterReading> findLatestPerMeterSince(@Param("since") Instant since);
}

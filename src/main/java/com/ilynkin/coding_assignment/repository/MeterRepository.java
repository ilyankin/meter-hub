package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.Meter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsBySerialNumber(String serialNumber);

    Page<Meter> findByUserId(Long userId, Pageable pageable);

    long deleteMeterById(long id);

    List<Meter> findBySerialNumberIn(Collection<String> serialNumbers);
}

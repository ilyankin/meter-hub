package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.dto.request.MeterRequest;
import com.ilynkin.coding_assignment.dto.response.MeterResponse;
import com.ilynkin.coding_assignment.entity.Meter;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.exception.DuplicateResourceException;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.mapper.MeterMapper;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterService {

    private final MeterRepository meterRepository;
    private final UserRepository userRepository;
    private final MeterMapper meterMapper;

    public MeterResponse findById(Long id) {
        return meterRepository.findById(id)
                .map(meterMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
    }

    public Page<MeterResponse> findAll(Long userId, Pageable pageable) {
        Page<Meter> meters = (userId == null)
                ? meterRepository.findAll(pageable)
                : meterRepository.findByUserId(userId, pageable);
        return meters.map(meterMapper::toResponse);
    }

    @Transactional
    public MeterResponse create(MeterRequest request) {
        if (meterRepository.existsBySerialNumber(request.serialNumber())) {
            throw new DuplicateResourceException(
                    "Meter with serial number " + request.serialNumber() + " already exists");
        }

        Meter meter = meterMapper.toEntity(request);
        meter.setUser(getUser(request.userId()));

        return meterMapper.toResponse(meterRepository.saveAndFlush(meter));
    }

    @Transactional
    public MeterResponse update(Long id, MeterRequest request) {
        Meter meter = meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));

        if (!meter.getSerialNumber().equals(request.serialNumber())
                && meterRepository.existsBySerialNumber(request.serialNumber())) {
            throw new DuplicateResourceException(
                    "Meter with serial number " + request.serialNumber() + " already exists");
        }

        meterMapper.updateEntity(request, meter);
        meter.setUser(getUser(request.userId()));

        return meterMapper.toResponse(meterRepository.save(meter));
    }

    @Transactional
    public void delete(Long id) {
        if (meterRepository.deleteMeterById(id) == 0) {
            throw new ResourceNotFoundException("Meter not found");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

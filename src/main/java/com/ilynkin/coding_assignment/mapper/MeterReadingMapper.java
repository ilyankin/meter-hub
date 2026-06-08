package com.ilynkin.coding_assignment.mapper;

import com.ilynkin.coding_assignment.dto.response.MeterReadingResponse;
import com.ilynkin.coding_assignment.dto.response.MeterReadingValueResponse;
import com.ilynkin.coding_assignment.entity.MeterReading;
import com.ilynkin.coding_assignment.entity.MeterReadingValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeterReadingMapper {

    @Mapping(target = "meterId", source = "meter.id")
    MeterReadingResponse toResponse(MeterReading reading);

    @Mapping(target = "tariffZone", source = "tariffZone.code")
    @Mapping(target = "value", source = "readingValue")
    MeterReadingValueResponse toValueResponse(MeterReadingValue value);
}

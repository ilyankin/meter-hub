package com.ilynkin.coding_assignment.mapper;

import com.ilynkin.coding_assignment.dto.request.MeterRequest;
import com.ilynkin.coding_assignment.dto.response.MeterResponse;
import com.ilynkin.coding_assignment.entity.Meter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MeterMapper {

    @Mapping(target = "userId", source = "user.id")
    MeterResponse toResponse(Meter meter);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Meter toEntity(MeterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(MeterRequest request, @MappingTarget Meter meter);
}

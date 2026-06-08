package com.ilynkin.coding_assignment.dto.response;

import com.ilynkin.coding_assignment.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
}

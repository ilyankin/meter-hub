package com.ilynkin.coding_assignment.dto.response;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}

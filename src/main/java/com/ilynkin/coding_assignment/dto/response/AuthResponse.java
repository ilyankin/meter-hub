package com.ilynkin.coding_assignment.dto.response;

import com.ilynkin.coding_assignment.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        Role role
) {
}

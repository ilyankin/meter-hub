package com.ilynkin.coding_assignment.dto.request;

import com.ilynkin.coding_assignment.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email @Size(max = 255)
        String email,
        @NotBlank @Size(max = 255)
        String fullName,
        @NotBlank @Size(min = 8, max = 255)
        String password,
        @NotNull Role role
) {
}

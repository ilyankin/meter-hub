package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.request.UserRequest;
import com.ilynkin.coding_assignment.dto.response.UserResponse;
import com.ilynkin.coding_assignment.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Управление пользователями (только для администратора)")
public class UserController {
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Список пользователей", description = "Возвращает всех пользователей с пагинацией")
    public PagedModel<UserResponse> findAll(Pageable pageable) {
        return new PagedModel<>(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Информация о пользователе", description = "Возвращает данные пользователя по ID")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Создание пользователя", description = "Создаёт нового пользователя с указанной ролью")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        URI location = URI.create(ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .getPath());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Редактирование пользователя", description = "Обновляет данные пользователя по ID")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление пользователя", description = "Удаляет пользователя по ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

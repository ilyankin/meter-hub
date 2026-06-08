package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.request.MeterRequest;
import com.ilynkin.coding_assignment.dto.response.MeterResponse;
import com.ilynkin.coding_assignment.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Приборы учёта", description = "Управление приборами учёта (только для администратора)")
public class MeterController {

    private final MeterService meterService;

    @GetMapping
    @Operation(summary = "Список приборов", description = "Возвращает приборы учёта с пагинацией, опционально по ответственному пользователю")
    public PagedModel<MeterResponse> findAll(@RequestParam(required = false) Long userId, Pageable pageable) {
        return new PagedModel<>(meterService.findAll(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Информация о приборе", description = "Возвращает прибор учёта по ID")
    public ResponseEntity<MeterResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(meterService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Создание прибора", description = "Создаёт прибор учёта с привязкой к ответственному пользователю")
    public ResponseEntity<MeterResponse> create(@Valid @RequestBody MeterRequest request) {
        MeterResponse response = meterService.create(request);
        URI location = URI.create(ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .getPath());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Редактирование прибора", description = "Обновляет данные прибора учёта по ID")
    public ResponseEntity<MeterResponse> update(@PathVariable Long id, @Valid @RequestBody MeterRequest request) {
        return ResponseEntity.ok(meterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление прибора", description = "Удаляет прибор учёта по ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        meterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

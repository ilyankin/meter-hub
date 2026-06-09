package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.request.MeterReadingRequest;
import com.ilynkin.coding_assignment.dto.response.CsvImportResponse;
import com.ilynkin.coding_assignment.dto.response.MeterReadingResponse;
import com.ilynkin.coding_assignment.service.CsvImportService;
import com.ilynkin.coding_assignment.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Показания", description = "Управление показаниями приборов учёта (администратор и менеджер)")
public class MeterReadingController {

    private final MeterReadingService readingService;
    private final CsvImportService csvImportService;

    @GetMapping
    @Operation(summary = "Список показаний", description = "Возвращает показания с пагинацией, опционально по прибору учёта")
    public PagedModel<MeterReadingResponse> findAll(@RequestParam(required = false) Long meterId, Pageable pageable) {
        return new PagedModel<>(readingService.findAll(meterId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Информация о показании", description = "Возвращает показание по ID")
    public ResponseEntity<MeterReadingResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(readingService.findById(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Импорт показаний из CSV",
            description = "Загружает показания файлом (поле file). Формат: serial,date,<зоны>. "
                    + "Всё или ничего: любая ошибка отклоняет весь файл (только администратор).")
    public ResponseEntity<CsvImportResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(csvImportService.importReadings(file));
    }

    @PostMapping
    @Operation(summary = "Создание показания", description = "Создаёт показание прибора учёта со значениями по тарифным зонам")
    public ResponseEntity<MeterReadingResponse> create(@Valid @RequestBody MeterReadingRequest request) {
        MeterReadingResponse response = readingService.create(request);
        URI location = URI.create(ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .getPath());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Редактирование показания", description = "Обновляет показание по ID")
    public ResponseEntity<MeterReadingResponse> update(@PathVariable UUID id, @Valid @RequestBody MeterReadingRequest request) {
        return ResponseEntity.ok(readingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление показания", description = "Удаляет показание по ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        readingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.ReportSendResponse;
import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import com.ilynkin.coding_assignment.service.ReportDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Отчёты", description = "Отчёты с показаниями приборов учёта (администратор и менеджер)")
public class ReportController {

    private final ReportDispatchService dispatchService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Отправить отчёт сейчас",
            description = "Формирует CSV с последними показаниями всех приборов и сразу отправляет его "
                    + "на адрес из app.report.recipient (администратор и менеджер).")
    public ResponseEntity<ReportSendResponse> sendNow() {
        ReportFile report = dispatchService.dispatchLatestReadingsReport();
        return ResponseEntity.ok(new ReportSendResponse(report.filename(), report.content().length));
    }
}

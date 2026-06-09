package com.ilynkin.coding_assignment.scheduling;

import com.ilynkin.coding_assignment.service.ReportDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportDispatchService dispatchService;

    @Scheduled(fixedRateString = "${app.report.interval}", initialDelayString = "${app.report.initial-delay}")
    public void sendLatestReadingsReport() {
        try {
            dispatchService.dispatchLatestReadingsReport();
        } catch (Exception ex) {
            log.error("Не удалось сформировать/отправить отчёт с показаниями", ex);
        }
    }
}

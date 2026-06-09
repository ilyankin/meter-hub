package com.ilynkin.coding_assignment.service;

import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportDispatchService {

    private final MeterReadingReportService reportService;
    private final ReportEmailSender emailSender;

    public ReportFile dispatchLatestReadingsReport() {
        ReportFile report = reportService.buildLatestReadingsReport();
        emailSender.send(report);
        return report;
    }
}

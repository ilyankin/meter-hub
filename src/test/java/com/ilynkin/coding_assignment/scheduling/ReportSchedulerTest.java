package com.ilynkin.coding_assignment.scheduling;

import com.ilynkin.coding_assignment.service.MeterReadingReportService.ReportFile;
import com.ilynkin.coding_assignment.service.ReportDispatchService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSchedulerTest {

    private final ReportDispatchService dispatchService = mock(ReportDispatchService.class);
    private final ReportScheduler scheduler = new ReportScheduler(dispatchService);

    @Test
    void run_dispatchesReport() {
        when(dispatchService.dispatchLatestReadingsReport())
                .thenReturn(new ReportFile("r.csv", new byte[0], 14));

        scheduler.sendLatestReadingsReport();

        verify(dispatchService).dispatchLatestReadingsReport();
    }

    @Test
    void run_swallowsFailures_soSchedulerKeepsAlive() {
        when(dispatchService.dispatchLatestReadingsReport()).thenThrow(new RuntimeException("boom"));

        assertThatCode(scheduler::sendLatestReadingsReport).doesNotThrowAnyException();
    }
}

package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.MeterReadingResponse;
import com.ilynkin.coding_assignment.dto.response.MeterReadingValueResponse;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.service.MeterReadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(MeterReadingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeterReadingControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final UUID READING_ID = UUID.fromString("0190a0e0-0000-7000-8000-000000000001");

    private static final String VALID_BODY = """
            {"meterId":1,"readingDate":"2024-01-15T00:00:00Z",
             "values":[{"tariffZone":"T1","value":100.5},{"tariffZone":"T2","value":50.25}]}
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private MeterReadingService readingService;

    private static MeterReadingResponse sampleResponse() {
        return new MeterReadingResponse(
                READING_ID, 1L, Instant.parse("2024-01-15T00:00:00Z"),
                List.of(new MeterReadingValueResponse("T1", new BigDecimal("100.500")),
                        new MeterReadingValueResponse("T2", new BigDecimal("50.250"))),
                Instant.now());
    }

    @Test
    void create_returns201WithLocation() {
        when(readingService.create(any())).thenReturn(sampleResponse());

        MvcTestResult result = mockMvc.post().uri("/api/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/readings/" + READING_ID);

        assertThat(result).bodyJson().convertTo(MeterReadingResponse.class)
                .satisfies(body -> {
                    assertThat(body.id()).isEqualTo(READING_ID);
                    assertThat(body.meterId()).isEqualTo(1L);
                    assertThat(body.values()).hasSize(2);
                });
    }

    @Test
    void findById_returns200() {
        when(readingService.findById(READING_ID)).thenReturn(sampleResponse());

        assertThat(mockMvc.get().uri("/api/readings/{id}", READING_ID))
                .hasStatusOk()
                .bodyJson().extractingPath("$.values[0].tariffZone").asString().isEqualTo("T1");
    }

    @Test
    void findById_missing_returns404ProblemDetail() {
        when(readingService.findById(READING_ID))
                .thenThrow(new ResourceNotFoundException("Reading not found"));

        MvcTestResult result = mockMvc.get().uri("/api/readings/{id}", READING_ID).exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().isEqualTo("Reading not found");
    }

    @Test
    void findAll_returns200WithContent() {
        when(readingService.findAll(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        assertThat(mockMvc.get().uri("/api/readings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].meterId").asNumber().isEqualTo(1);
    }

    @Test
    void findAll_filtersByMeterId() {
        when(readingService.findAll(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(mockMvc.get().uri("/api/readings?meterId=7"))
                .hasStatusOk();

        verify(readingService).findAll(eq(7L), any(Pageable.class));
    }

    @Test
    void create_unknownMeter_returns404ProblemDetail() {
        when(readingService.create(any()))
                .thenThrow(new ResourceNotFoundException("Meter not found"));

        assertThat(mockMvc.post().uri("/api/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void create_emptyValues_returns400ProblemDetail() {
        String invalid = """
                {"meterId":1,"values":[]}
                """;

        assertThat(mockMvc.post().uri("/api/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);

        verifyNoInteractions(readingService);
    }

    @Test
    void create_missingMeterId_returns400ProblemDetail() {
        String invalid = """
                {"values":[{"tariffZone":"T1","value":100.5}]}
                """;

        assertThat(mockMvc.post().uri("/api/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);

        verifyNoInteractions(readingService);
    }

    @Test
    void update_returns200() {
        when(readingService.update(eq(READING_ID), any())).thenReturn(sampleResponse());

        assertThat(mockMvc.put().uri("/api/readings/{id}", READING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatusOk();
    }

    @Test
    void update_missing_returns404ProblemDetail() {
        when(readingService.update(eq(READING_ID), any()))
                .thenThrow(new ResourceNotFoundException("Reading not found"));

        assertThat(mockMvc.put().uri("/api/readings/{id}", READING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void delete_returns204() {
        assertThat(mockMvc.delete().uri("/api/readings/{id}", READING_ID))
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(readingService).delete(READING_ID);
    }

    @Test
    void delete_missing_returns404ProblemDetail() {
        doThrow(new ResourceNotFoundException("Reading not found"))
                .when(readingService).delete(READING_ID);

        assertThat(mockMvc.delete().uri("/api/readings/{id}", READING_ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }
}

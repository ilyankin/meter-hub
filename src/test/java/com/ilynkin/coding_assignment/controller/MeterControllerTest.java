package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.MeterResponse;
import com.ilynkin.coding_assignment.exception.DuplicateResourceException;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.service.MeterService;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(MeterController.class)
@AutoConfigureMockMvc(addFilters = false)
class MeterControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String VALID_BODY = """
            {"userId":1,"serialNumber":"SN-001","inventoryNumber":"INV-1","manufactureYear":2020,
             "transformationRatio":1.5,"installationDate":"2024-01-15","sealNumber":"S-1",
             "antimagneticSealNumber":"AM-1","installationLocation":"Подвал","notes":"примечание","gisId":"GIS-1"}
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private MeterService meterService;

    private static MeterResponse sampleResponse() {
        return new MeterResponse(
                1L, 1L, "SN-001", "INV-1", 2020, new BigDecimal("1.50"),
                LocalDate.of(2024, 1, 15), "S-1", "AM-1", "Подвал", "примечание", "GIS-1",
                Instant.now(), Instant.now());
    }

    @Test
    void create_returns201WithLocation() {
        when(meterService.create(any())).thenReturn(sampleResponse());

        MvcTestResult result = mockMvc.post().uri("/api/meters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/meters/1");

        assertThat(result).bodyJson().convertTo(MeterResponse.class)
                .satisfies(body -> {
                    assertThat(body.id()).isEqualTo(1L);
                    assertThat(body.serialNumber()).isEqualTo("SN-001");
                    assertThat(body.userId()).isEqualTo(1L);
                });
    }

    @Test
    void findById_returns200() {
        when(meterService.findById(1L)).thenReturn(sampleResponse());

        assertThat(mockMvc.get().uri("/api/meters/1"))
                .hasStatusOk()
                .bodyJson().convertTo(MeterResponse.class)
                .satisfies(body -> assertThat(body.serialNumber()).isEqualTo("SN-001"));
    }

    @Test
    void findById_missing_returns404ProblemDetail() {
        when(meterService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Meter not found"));

        MvcTestResult result = mockMvc.get().uri("/api/meters/99").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().isEqualTo("Meter not found");
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(404);
    }

    @Test
    void findAll_returns200WithContent() {
        when(meterService.findAll(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        assertThat(mockMvc.get().uri("/api/meters"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].serialNumber").asString().isEqualTo("SN-001");
    }

    @Test
    void findAll_filtersByUserId() {
        when(meterService.findAll(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(mockMvc.get().uri("/api/meters?userId=5"))
                .hasStatusOk();

        verify(meterService).findAll(eq(5L), any(Pageable.class));
    }

    @Test
    void create_duplicateSerial_returns409ProblemDetail() {
        when(meterService.create(any()))
                .thenThrow(new DuplicateResourceException("Meter with serial number SN-001 already exists"));

        assertThat(mockMvc.post().uri("/api/meters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").asNumber().isEqualTo(409);
    }

    @Test
    void create_unknownResponsibleUser_returns404ProblemDetail() {
        when(meterService.create(any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThat(mockMvc.post().uri("/api/meters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void create_invalidBody_returns400ProblemDetail() {
        String invalid = """
                {"userId":null,"serialNumber":""}
                """;

        assertThat(mockMvc.post().uri("/api/meters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);

        verifyNoInteractions(meterService);
    }

    @Test
    void update_returns200() {
        when(meterService.update(eq(1L), any())).thenReturn(sampleResponse());

        assertThat(mockMvc.put().uri("/api/meters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatusOk();
    }

    @Test
    void update_missing_returns404ProblemDetail() {
        when(meterService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Meter not found"));

        assertThat(mockMvc.put().uri("/api/meters/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void delete_returns204() {
        assertThat(mockMvc.delete().uri("/api/meters/1"))
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(meterService).delete(1L);
    }

    @Test
    void delete_missing_returns404ProblemDetail() {
        doThrow(new ResourceNotFoundException("Meter not found"))
                .when(meterService).delete(99L);

        assertThat(mockMvc.delete().uri("/api/meters/99"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }
}

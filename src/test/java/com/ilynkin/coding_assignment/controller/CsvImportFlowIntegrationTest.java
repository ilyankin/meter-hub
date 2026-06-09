package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.AuthResponse;
import com.ilynkin.coding_assignment.entity.Meter;
import com.ilynkin.coding_assignment.entity.User;
import com.ilynkin.coding_assignment.repository.MeterRepository;
import com.ilynkin.coding_assignment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc
class CsvImportFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String MANAGER_EMAIL = "manager@example.com";
    private static final String MANAGER_PASSWORD = "manager123";

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeterRepository meterRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String serial;

    @BeforeEach
    void setUp() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        serial = "SN-IMP-" + System.nanoTime();
        meterRepository.save(Meter.builder()
                .user(manager)
                .serialNumber(serial)
                .build());
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "readings.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void admin_validCsv_imports() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        MockMultipartFile csv = file("""
                serial,date,T1,T2
                %s,2024-01-15,100.5,50.25
                """.formatted(serial));

        MvcTestResult result = mockMvc.perform(multipart("/api/readings/import")
                .file(csv)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(result).hasStatusOk()
                .bodyJson()
                .extractingPath("$.imported").isEqualTo(1);
    }

    @Test
    void manager_forbidden() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);
        MockMultipartFile csv = file("""
                serial,date,T1
                %s,2024-01-15,5
                """.formatted(serial));

        MvcTestResult result = mockMvc.perform(multipart("/api/readings/import")
                .file(csv)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_unknownSerial_returns400_withErrors() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        MockMultipartFile csv = file("""
                serial,date,T1
                NOPE-404,2024-01-15,5
                """);

        MvcTestResult result = mockMvc.perform(multipart("/api/readings/import")
                .file(csv)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors").asArray().isNotEmpty();
    }

    @Test
    void admin_inFileDuplicate_returns400() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        MockMultipartFile csv = file("""
                serial,date,T1
                %s,2024-01-15,5
                %s,2024-01-15,6
                """.formatted(serial, serial));

        MvcTestResult result = mockMvc.perform(multipart("/api/readings/import")
                .file(csv)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private String login(String email, String password) throws Exception {
        MvcTestResult result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .exchange();
        assertThat(result).hasStatusOk();
        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.token();
    }
}

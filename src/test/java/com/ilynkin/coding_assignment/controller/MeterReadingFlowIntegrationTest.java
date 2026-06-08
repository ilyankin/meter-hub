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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class MeterReadingFlowIntegrationTest {

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

    private Long meterId;

    @BeforeEach
    void setUp() {
        User manager = userRepository.findByEmail(MANAGER_EMAIL).orElseThrow();
        Meter meter = meterRepository.save(Meter.builder()
                .user(manager)
                .serialNumber("SN-INT-" + System.nanoTime())
                .build());
        meterId = meter.getId();
    }

    @Test
    void manager_createsAndReadsBackReading_withTariffValues() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);

        String body = """
                {"meterId":%d,"readingDate":"2024-01-15T00:00:00Z",
                 "values":[{"tariffZone":"T1","value":100.5},{"tariffZone":"T2","value":50.25}]}
                """.formatted(meterId);

        MvcTestResult created = mockMvc.post().uri("/api/readings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();

        assertThat(created).hasStatus(org.springframework.http.HttpStatus.CREATED);
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asString();

        assertThat(mockMvc.get().uri("/api/readings/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.values").asArray().hasSize(2);
    }

    @Test
    void manager_unknownTariffZone_returns404() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);

        String body = """
                {"meterId":%d,"values":[{"tariffZone":"T9","value":1.0}]}
                """.formatted(meterId);

        assertThat(mockMvc.post().uri("/api/readings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(org.springframework.http.HttpStatus.NOT_FOUND);
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

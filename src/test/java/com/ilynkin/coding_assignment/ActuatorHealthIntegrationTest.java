package com.ilynkin.coding_assignment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void health_withoutToken_returns200Up() {
        assertThat(mockMvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").asString().isEqualTo("UP");
    }

    @Test
    void health_withoutToken_hidesDetails() {
        assertThat(mockMvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().doesNotHavePath("$.components");
    }

    @Test
    void info_withoutToken_returns401() {
        assertThat(mockMvc.get().uri("/actuator/info"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}

package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.AuthResponse;
import com.ilynkin.coding_assignment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String MANAGER_EMAIL = "manager@example.com";
    private static final String MANAGER_PASSWORD = "manager123";

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminLogsInAndAccessesUsers_returns200() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long adminId = userService.findByEmail(ADMIN_EMAIL).id();

        assertThat(mockMvc.get().uri("/api/users/{id}", adminId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson().extractingPath("$.role").asString().isEqualTo("ADMIN");
    }

    @Test
    void managerToken_cannotAccessUsers_returns403() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);

        assertThat(mockMvc.get().uri("/api/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void managerToken_cannotAccessMeters_returns403() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);

        assertThat(mockMvc.get().uri("/api/meters")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void managerToken_getsOwnProfile_viaMe() throws Exception {
        String token = login(MANAGER_EMAIL, MANAGER_PASSWORD);

        assertThat(mockMvc.get().uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson().extractingPath("$.email").asString().isEqualTo(MANAGER_EMAIL);
    }

    @Test
    void me_withoutToken_returns401() {
        assertThat(mockMvc.get().uri("/api/users/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void noToken_returns401() {
        assertThat(mockMvc.get().uri("/api/users/1"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidToken_returns401() {
        assertThat(mockMvc.get().uri("/api/users/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private String login(String email, String password) throws Exception {
        MvcTestResult result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .exchange();
        assertThat(result).hasStatusOk();
        return extractToken(result);
    }

    private String extractToken(MvcTestResult result) throws Exception {
        AuthResponse body = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return body.token();
    }
}

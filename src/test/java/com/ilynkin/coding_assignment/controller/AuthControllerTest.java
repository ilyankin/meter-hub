package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.AuthResponse;
import com.ilynkin.coding_assignment.entity.Role;
import com.ilynkin.coding_assignment.repository.AuthTokenRepository;
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
class AuthControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String MANAGER_EMAIL = "manager@example.com";
    private static final String MANAGER_PASSWORD = "manager123";

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_returns200WithToken() {
        MvcTestResult result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(MANAGER_EMAIL, MANAGER_PASSWORD))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().convertTo(AuthResponse.class)
                .satisfies(r -> {
                    assertThat(r.token()).isNotBlank();
                    assertThat(r.role()).isEqualTo(Role.MANAGER);
                });
    }

    @Test
    void login_invalidCredentials_returns401ProblemDetail() {
        MvcTestResult result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(MANAGER_EMAIL, "wrong-password"))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(401);
    }

    @Test
    void login_invalidBody_returns400ProblemDetail() {
        String invalid = """
                {"email":"not-an-email","password":""}
                """;

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void logout_deletesTokenFromDb_returns204() throws Exception {
        MvcTestResult login = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(MANAGER_EMAIL, MANAGER_PASSWORD))
                .exchange();
        String token = objectMapper.readValue(
                login.getResponse().getContentAsString(), AuthResponse.class).token();
        assertThat(authTokenRepository.findByToken(token)).isPresent();

        assertThat(mockMvc.post().uri("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(authTokenRepository.findByToken(token)).isEmpty();
    }

    private static String loginBody(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }
}

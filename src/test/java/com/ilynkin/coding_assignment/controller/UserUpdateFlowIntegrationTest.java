package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.AuthResponse;
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
class UserUpdateFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin12345";

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateWithoutPassword_keepsOldPasswordWorking() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "upd-" + System.nanoTime() + "@example.com";
        String password = "secret12";

        MvcTestResult created = mockMvc.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","fullName":"До правки","password":"%s","role":"MANAGER"}
                        """.formatted(email, password))
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        assertThat(mockMvc.put().uri("/api/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","fullName":"После правки","role":"MANAGER"}
                        """.formatted(email)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.fullName").asString().isEqualTo("После правки");

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .hasStatusOk();
    }

    @Test
    void updateWithPassword_changesPassword() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "updpwd-" + System.nanoTime() + "@example.com";

        MvcTestResult created = mockMvc.post().uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","fullName":"Пользователь","password":"oldpass12","role":"MANAGER"}
                        """.formatted(email))
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        assertThat(mockMvc.put().uri("/api/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","fullName":"Пользователь","password":"newpass12","role":"MANAGER"}
                        """.formatted(email)))
                .hasStatusOk();

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"newpass12\"}".formatted(email)))
                .hasStatusOk();

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"oldpass12\"}".formatted(email)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
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

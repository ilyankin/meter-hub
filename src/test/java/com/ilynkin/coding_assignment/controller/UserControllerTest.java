package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.dto.response.UserResponse;
import com.ilynkin.coding_assignment.exception.DuplicateResourceException;
import com.ilynkin.coding_assignment.exception.ResourceNotFoundException;
import com.ilynkin.coding_assignment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String VALID_BODY = """
            {"email":"ivan@example.com","fullName":"Иван Иванов","password":"secret12","role":"ADMIN"}
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void create_returns201WithLocationAndNoPassword() {
        UserResponse response = new UserResponse(
                1L, "ivan@example.com", "Иван Иванов", "ADMIN", Instant.now(), Instant.now());
        when(userService.create(any())).thenReturn(response);

        MvcTestResult result = mockMvc.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/users/1");

        assertThat(result).bodyJson().convertTo(UserResponse.class)
                .satisfies(body -> {
                    assertThat(body.id()).isEqualTo(1L);
                    assertThat(body.email()).isEqualTo("ivan@example.com");
                    assertThat(body.role()).isEqualTo("ADMIN");
                });

        assertThat(result).bodyJson().doesNotHavePath("$.password");
    }

    @Test
    void findById_returns200() {
        UserResponse response = new UserResponse(
                1L, "ivan@example.com", "Иван Иванов", "MANAGER", Instant.now(), Instant.now());
        when(userService.findById(1L)).thenReturn(response);

        assertThat(mockMvc.get().uri("/api/users/1"))
                .hasStatusOk()
                .bodyJson().convertTo(UserResponse.class)
                .satisfies(body -> assertThat(body.role()).isEqualTo("MANAGER"));
    }

    @Test
    void findById_missing_returns404ProblemDetail() {
        when(userService.findById(99L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        MvcTestResult result = mockMvc.get().uri("/api/users/99").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.title").asString().isEqualTo("Resource not found");
        assertThat(result).bodyJson().extractingPath("$.detail").asString().isEqualTo("User not found");
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(404);
    }

    @Test
    void create_duplicate_returns409ProblemDetail() {
        when(userService.create(any()))
                .thenThrow(new DuplicateResourceException("User with email ivan@example.com already exists"));

        assertThat(mockMvc.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").asNumber().isEqualTo(409);
    }

    @Test
    void create_invalidBody_returns400ProblemDetail() {
        String invalid = """
                {"email":"not-an-email","fullName":"","password":"short","role":""}
                """;

        assertThat(mockMvc.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);

        verifyNoInteractions(userService);
    }

    @Test
    void unexpectedError_returns500GenericProblemDetail() {
        when(userService.findById(1L)).thenThrow(new RuntimeException("connection pool exhausted at jdbc:..."));

        MvcTestResult result = mockMvc.get().uri("/api/users/1").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);

        assertThat(result).bodyJson().extractingPath("$.detail").asString()
                .isEqualTo("Internal server error")
                .doesNotContain("jdbc");
    }

    @Test
    void update_returns200() {
        UserResponse response = new UserResponse(
                1L, "ivan@example.com", "Иван Иванов", "ADMIN", Instant.now(), Instant.now());
        when(userService.update(eq(1L), any())).thenReturn(response);

        assertThat(mockMvc.put().uri("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatusOk();
    }

    @Test
    void update_missing_returns404ProblemDetail() {
        when(userService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThat(mockMvc.put().uri("/api/users/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }

    @Test
    void delete_returns204() {
        assertThat(mockMvc.delete().uri("/api/users/1"))
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(userService).delete(1L);
    }

    @Test
    void delete_missing_returns404ProblemDetail() {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).delete(99L);

        assertThat(mockMvc.delete().uri("/api/users/99"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(PROBLEM_JSON);
    }
}

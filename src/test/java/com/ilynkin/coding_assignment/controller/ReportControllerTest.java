package com.ilynkin.coding_assignment.controller;

import com.ilynkin.coding_assignment.service.ReportEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportEmailSender emailSender;

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canTriggerReport() throws Exception {
        mockMvc.perform(post("/api/reports/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename", startsWith("meter-readings-")))
                .andExpect(jsonPath("$.sizeBytes").exists());

        verify(emailSender).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_canTriggerReport() throws Exception {
        mockMvc.perform(post("/api/reports/send"))
                .andExpect(status().isOk());

        verify(emailSender).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/api/reports/send"))
                .andExpect(status().isUnauthorized());
    }
}

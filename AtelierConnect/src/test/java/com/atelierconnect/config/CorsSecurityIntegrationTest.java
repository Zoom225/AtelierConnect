package com.atelierconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsSecurityIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void browserCanPreflightProtectedEndpointWithoutSendingItsToken() throws Exception {
        mvc.perform(options("/api/reservations")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void allowingPreflightDoesNotAuthorizeActualAnonymousWrite() throws Exception {
        mvc.perform(post("/api/reservations")
                        .header("Origin", "http://localhost:4200")
                        .contentType("application/json")
                        .content("{\"atelierId\":1,\"participantId\":1,\"requesterId\":1}"))
                .andExpect(status().isForbidden());
    }
}

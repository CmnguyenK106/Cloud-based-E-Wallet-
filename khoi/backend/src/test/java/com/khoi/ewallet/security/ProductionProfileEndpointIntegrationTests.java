package com.khoi.ewallet.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "spring.datasource.url=jdbc:mysql://localhost:3307/ewallet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh",
        "spring.datasource.username=ewallet_user",
        "spring.datasource.password=ewallet_pass",
        "jwt.secret=test-only-secret!that-is-at-least-32-bytes-long",
        "frontend.base-url=https://wallet.example.test",
        "app.cors.allowed-origins=https://wallet.example.test"
})
@AutoConfigureMockMvc
class ProductionProfileEndpointIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testEndpointsAreNotRegisteredInProduction() throws Exception {
        mockMvc.perform(get("/api/test/ping"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicProductionEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedProductionEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/account/me"))
                .andExpect(status().isUnauthorized());
    }
}

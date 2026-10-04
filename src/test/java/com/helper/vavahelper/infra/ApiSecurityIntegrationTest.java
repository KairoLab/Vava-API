package com.helper.vavahelper.infra;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicCatalogIsOpenCachedAndHasEtag() throws Exception {
        mvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(header().exists("ETag"));
        mvc.perform(get("/media")).andExpect(status().isOk());
    }

    @Test
    void unknownAgentIs404() throws Exception {
        mvc.perform(get("/agents/ghost")).andExpect(status().isNotFound());
        mvc.perform(get("/agents/ghost/with-skills")).andExpect(status().isNotFound());
    }

    @Test
    void invalidBearerTokenDoesNotBreakPublicRoutes() throws Exception {
        mvc.perform(get("/agents").header("Authorization", "Bearer junk"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoutesRequireAuthentication() throws Exception {
        mvc.perform(get("/qualquer-coisa")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerAndH2AreNotExposedByDefault() throws Exception {
        mvc.perform(get("/docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2/")).andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/agents"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));
    }

    @Test
    void corsAllowsOnlyConfiguredOrigin() throws Exception {
        mvc.perform(get("/agents").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(get("/agents").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginValidationAndBadCredentials() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ninguem@example.com\",\"password\":\"Whatever1!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void registerThenLogin() throws Exception {
        String weak = "{\"login\":\"novo@example.com\",\"password\":\"123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(weak))
                .andExpect(status().isBadRequest());

        String ok = "{\"login\":\"novo@example.com\",\"password\":\"Senha-Forte1\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherEmailExists() throws Exception {
        mvc.perform(post("/auth/forgot-password").param("email", "desconhecido@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPasswordWithUnknownTokenIs400WithoutLeakingDetails() throws Exception {
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"nao-existe\",\"newPassword\":\"Senha-Forte1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))));
    }
}

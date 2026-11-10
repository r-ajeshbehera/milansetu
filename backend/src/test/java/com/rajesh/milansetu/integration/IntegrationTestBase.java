package com.rajesh.milansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajesh.milansetu.notification.MockNotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MySQL 8 (Testcontainers, singleton) + Flyway + demo data. Needs Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    /**
     * Database for integration tests:
     *  - default (CI): one MySQL 8.4 container for the whole run (Testcontainers, needs Docker)
     *  - TEST_DB_URL / TEST_DB_USER / TEST_DB_PASSWORD set: an existing empty MySQL 8 database instead
     */
    static final String EXTERNAL_URL = System.getenv("TEST_DB_URL");
    static final MySQLContainer<?> MYSQL = EXTERNAL_URL == null ? new MySQLContainer<>("mysql:8.4") : null;

    static {
        if (MYSQL != null) MYSQL.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (MYSQL != null) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USER"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected MockNotificationSender outbox;

    protected static final String ADMIN = "admin@milansetu.demo";
    protected static final String POLICE = "police@milansetu.demo";
    protected static final String DESK1 = "desk1@milansetu.demo";
    protected static final String DESK2 = "desk2@milansetu.demo";
    protected static final String VOLUNTEER = "volunteer@milansetu.demo";
    protected static final String FAMILY1 = "family1@milansetu.demo";
    protected static final String FAMILY2 = "family2@milansetu.demo";
    protected static final String RIYA_TOKEN = "3f2a9c1e-7b4d-4e8a-9c21-5d6f7a8b9c01";
    protected static final String BHAGABAN_TOKEN = "8d1e2f3a-4b5c-4d6e-8f70-1a2b3c4d5e02";

    protected String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo@1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    protected JsonNode read(String s) throws Exception { return json.readTree(s); }

    /** Reads the newest OTP the mock sender "delivered" to a guardian (test-only access to the outbox). */
    protected String latestOtpFor(long guardianId) {
        return outbox.messagesFor(guardianId).stream()
                .map(m -> Pattern.compile("code is (\\d{6})").matcher(m.body()))
                .filter(Matcher::find).map(m -> m.group(1)).findFirst().orElseThrow();
    }
}

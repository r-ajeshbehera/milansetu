package com.rajesh.milansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full API workflows on real MySQL:
 *  A) missing report + manual found report -> suggestion -> confirm -> arrived -> wrong OTP -> right OTP -> REUNITED -> timeline
 *  B) QR wristband scan -> duplicate scan returns same case -> confirm -> handover
 */
class ReunificationWorkflowIT extends IntegrationTestBase {

    @Test
    void missingAndFoundReportsAreMatchedAndHandedOverWithOtp() throws Exception {
        String desk2 = token(DESK2);
        // demo data: case 1 (missing Arjun) and found report 1 at Gundicha desk score 90
        JsonNode suggestions = read(mvc.perform(get("/api/reports/missing/1/matches").header("Authorization", desk2))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(suggestions.get(0).get("foundReportId").asLong()).isEqualTo(1L);
        assertThat(suggestions.get(0).get("score").asInt()).isEqualTo(90);
        assertThat(suggestions.get(0).get("factors").size()).isEqualTo(5);

        mvc.perform(post("/api/cases/1/confirm-match").header("Authorization", desk2).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foundReportId\":1,\"note\":\"Father identified the boy by photo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.matchScore").value(90))
                .andExpect(jsonPath("$.otp").doesNotExist());
        mvc.perform(get("/api/cases/2").header("Authorization", desk2)).andExpect(status().isConflict());   // merged

        mvc.perform(post("/api/cases/1/handover").header("Authorization", desk2).contentType(MediaType.APPLICATION_JSON)
                .content("{\"otp\":\"000000\"}")).andExpect(status().isConflict());                    // not AT_DESK yet
        mvc.perform(post("/api/cases/1/arrived").header("Authorization", desk2)).andExpect(jsonPath("$.status").value("AT_DESK"));

        String otp = latestOtpFor(7L);   // family2 = user 7
        String wrong = otp.equals("000000") ? "111111" : "000000";
        mvc.perform(post("/api/cases/1/handover").header("Authorization", desk2).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + wrong + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"))
                .andExpect(jsonPath("$.details.attemptsLeft").value(4));
        mvc.perform(post("/api/cases/1/handover").header("Authorization", desk2).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REUNITED"));
        mvc.perform(post("/api/cases/1/handover").header("Authorization", desk2).contentType(MediaType.APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\"}")).andExpect(status().isConflict());              // reused

        JsonNode timeline = read(mvc.perform(get("/api/cases/1/timeline").header("Authorization", token(FAMILY2)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(timeline.toString()).contains("REPORTED", "MATCHED", "AT_DESK", "REUNITED");
        assertThat(timeline.toString()).doesNotContain("Gundicha Desk (Demo)");   // family sees roles, not staff names
    }

    @Test
    void qrScanWorkflowWithDuplicateProtection() throws Exception {
        String volunteer = token(VOLUNTEER);
        JsonNode first = read(mvc.perform(post("/api/scan/" + RIYA_TOKEN).header("Authorization", volunteer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clothingColor\":\"PINK\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long caseId = first.get("caseId").asLong();
        assertThat(first.get("desk").get("name").asText()).isEqualTo("Singhadwara Help Desk");   // volunteer's home desk

        mvc.perform(post("/api/scan/" + RIYA_TOKEN).header("Authorization", volunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.newCase").value(false));

        String desk1 = token(DESK1);
        mvc.perform(post("/api/cases/" + caseId + "/confirm-match").header("Authorization", desk1)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(jsonPath("$.status").value("MATCHED"));
        mvc.perform(post("/api/cases/" + caseId + "/arrived").header("Authorization", desk1)).andExpect(status().isOk());
        mvc.perform(post("/api/cases/" + caseId + "/handover").header("Authorization", desk1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + latestOtpFor(6L) + "\"}"))
                .andExpect(jsonPath("$.status").value("REUNITED"));

        mvc.perform(get("/api/notifications").header("Authorization", token(FAMILY1)))
                .andExpect(jsonPath("$.content[*].type", org.hamcrest.Matchers.hasItems("MEMBER_FOUND", "OTP_SENT", "REUNITED")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("code is"))));
    }

    @Test
    void policeDashboardUsesRealData() throws Exception {
        mvc.perform(get("/api/dashboard/summary").param("eventId", "1").header("Authorization", token(POLICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMissing").isNumber())
                .andExpect(jsonPath("$.casesByStatus.REPORTED").isNumber());
    }
}

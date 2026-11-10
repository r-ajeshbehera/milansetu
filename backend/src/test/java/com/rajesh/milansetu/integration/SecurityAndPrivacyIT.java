package com.rajesh.milansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityAndPrivacyIT extends IntegrationTestBase {

    @Test void noTokenGives401() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
    }

    @Test void wrongRoleGives403() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", token(FAMILY1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"city\":\"Puri\",\"startDate\":\"2027-01-01\",\"endDate\":\"2027-01-02\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void publicRegistrationAlwaysCreatesFamily() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sneaky\",\"email\":\"sneaky@test.local\",\"phone\":\"9876543210\",\"password\":\"Password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FAMILY"));
    }

    @Test void invalidMissingReportGives400() throws Exception {
        mvc.perform(post("/api/reports/missing").header("Authorization", token(FAMILY1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"personName\":\"X\",\"approxAge\":200,\"gender\":\"MALE\",\"clothingColor\":\"RED\",\"lastSeenLat\":95,\"lastSeenLng\":85.8,\"lastSeenAt\":\"2027-07-07T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.approxAge").exists())
                .andExpect(jsonPath("$.fieldErrors.lastSeenLat").exists());
    }

    @Test void familyCannotSeeAnotherFamilysMember() throws Exception {
        // member 3 (Arjun) belongs to family2
        mvc.perform(get("/api/members/3").header("Authorization", token(FAMILY1))).andExpect(status().isNotFound());
        mvc.perform(get("/api/members/3/qr").header("Authorization", token(FAMILY1))).andExpect(status().isNotFound());
        mvc.perform(get("/api/members/3").header("Authorization", token(FAMILY2))).andExpect(status().isOk());
    }

    @Test void ownerGetsQrPng() throws Exception {
        mvc.perform(get("/api/members/1/qr").header("Authorization", token(FAMILY1)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test void volunteerScanNeverRevealsGuardianDetails() throws Exception {
        mvc.perform(post("/api/scan/MILANSETU:" + BHAGABAN_TOKEN).header("Authorization", token(VOLUNTEER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":19.8131,\"lng\":85.8301}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.firstName").value("Bhagaban"))
                .andExpect(jsonPath("$.desk.name").value("Gundicha Temple Help Desk"))
                .andExpect(content().string(not(containsString("9000000001"))))
                .andExpect(content().string(not(containsString("Mohanty"))))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.guardian").doesNotExist());
    }

    @Test void caseDetailsForDeskNeverContainPhoneOrNotes() throws Exception {
        mvc.perform(get("/api/cases").header("Authorization", token(DESK1)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("9000000002"))))
                .andExpect(content().string(not(containsString("peanuts"))));
    }

    @Test void invalidTokenGivesGenericMessage() throws Exception {
        mvc.perform(post("/api/scan/11111111-2222-3333-4444-555555555555").header("Authorization", token(VOLUNTEER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(startsWith("Wristband not recognised")));
        mvc.perform(post("/api/scan/hello").header("Authorization", token(VOLUNTEER)))
                .andExpect(status().isNotFound());
    }

    @Test void staffOfOtherEventCannotSeeItsCases() throws Exception {
        // desk1 is staff of event 1 only
        mvc.perform(get("/api/cases").param("eventId", "2").header("Authorization", token(DESK1))).andExpect(status().isNotFound());
    }

    @Test void familySeesOnlyOwnCases() throws Exception {
        mvc.perform(get("/api/cases").header("Authorization", token(FAMILY1)))
                .andExpect(jsonPath("$.content[*].personName", not(hasItem("Arjun Sahu"))));
        mvc.perform(get("/api/cases/1").header("Authorization", token(FAMILY1))).andExpect(status().isNotFound());
    }

    @Test void reunitedCannotBeSetWithoutOtp() throws Exception {
        // there is no endpoint that sets REUNITED directly; handover on a REPORTED case is refused
        mvc.perform(post("/api/cases/1/handover").header("Authorization", token(DESK1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isConflict());
    }
}

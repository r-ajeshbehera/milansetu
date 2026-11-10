package com.rajesh.milansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Admin set-up, reports without wristbands, wrong-match undo, police assignment/closure and family withdrawal. */
class AdminAndCaseManagementIT extends IntegrationTestBase {

    private static String minutesAgo(int m) {
        return LocalDateTime.now().minusMinutes(m).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private JsonNode post(String url, String token, String body, int expected) throws Exception {
        return read(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body == null ? "{}" : body))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }

    @Test
    void adminSetsUpEventDesksStaffAndUsers() throws Exception {
        String admin = token(ADMIN);
        JsonNode user = post("/api/users", admin, "{\"fullName\":\"New Volunteer\",\"email\":\"newvol@test.local\",\"password\":\"Password123\",\"role\":\"VOLUNTEER\"}", 201);
        post("/api/users", admin, "{\"fullName\":\"X\",\"email\":\"fam@test.local\",\"password\":\"Password123\",\"role\":\"FAMILY\"}", 422);   // families self-register
        mvc.perform(get("/api/users").param("role", "VOLUNTEER").header("Authorization", admin))
                .andExpect(jsonPath("$.content[*].email", hasItem("newvol@test.local")))
                .andExpect(jsonPath("$.content[0].phone").doesNotExist());

        JsonNode event = post("/api/events", admin, "{\"name\":\"Admin Test Mela (DEMO)\",\"city\":\"Cuttack\",\"startDate\":\"2026-01-01\",\"endDate\":\"2027-12-31\",\"active\":true}", 201);
        long eventId = event.get("id").asLong();
        post("/api/events", admin, "{\"name\":\"Bad dates\",\"city\":\"X\",\"startDate\":\"2027-02-01\",\"endDate\":\"2027-01-01\"}", 422);

        JsonNode desk = post("/api/events/" + eventId + "/desks", admin, "{\"name\":\"North Gate\",\"latitude\":20.46,\"longitude\":85.88,\"inChargeId\":3}", 201);
        assertThat(desk.get("inChargeName").asText()).contains("Singhadwara");
        post("/api/events/" + eventId + "/desks", admin, "{\"name\":\"Bad in-charge\",\"latitude\":20.46,\"longitude\":85.88,\"inChargeId\":5}", 422);  // volunteer cannot be in-charge
        mvc.perform(put("/api/events/" + eventId + "/staff/" + user.get("id").asLong()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"deskId\":" + desk.get("id").asLong() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deskId").value(desk.get("id").asLong()));
        mvc.perform(put("/api/events/" + eventId + "/staff/6").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());   // family is not staff
        mvc.perform(get("/api/events/" + eventId + "/staff").header("Authorization", admin))
                .andExpect(jsonPath("$[*].fullName", hasItems("New Volunteer", "Singhadwara Desk (Demo)")));
        mvc.perform(delete("/api/events/" + eventId + "/staff/" + user.get("id").asLong()).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(put("/api/events/" + eventId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Test Mela (DEMO)\",\"city\":\"Cuttack\",\"startDate\":\"2026-01-01\",\"endDate\":\"2027-12-31\",\"active\":false}"))
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/api/users/" + user.get("id").asLong()).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/api/users/1").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isUnprocessableEntity());   // cannot deactivate yourself
    }

    @Test
    void familyMemberLifecycleAndProfile() throws Exception {
        String fam = token(FAMILY1);
        JsonNode m = post("/api/members", fam, "{\"eventId\":1,\"fullName\":\"Test Grandma\",\"age\":80,\"gender\":\"FEMALE\",\"notes\":\"diabetic\"}", 201);
        long id = m.get("id").asLong();
        mvc.perform(get("/api/members").header("Authorization", fam)).andExpect(jsonPath("$[*].fullName", hasItem("Test Grandma")));
        mvc.perform(get("/api/members/" + id + "/wristband").header("Authorization", fam))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Test")))
                .andExpect(content().string(not(containsString("9000000001"))))
                .andExpect(content().string(not(containsString("diabetic"))));
        post("/api/members/" + id + "/qr/regenerate", fam, null, 200);
        mvc.perform(get("/api/users/me").header("Authorization", fam)).andExpect(jsonPath("$.phone").value("9000000001"));
        mvc.perform(patch("/api/users/me").header("Authorization", fam).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Family Mohanty (Demo)\"}")).andExpect(status().isOk());
    }

    @Test
    void wrongMatchIsUndoneAndCaseCanBeAssignedAndClosed() throws Exception {
        String fam = token(FAMILY2);
        // unregistered missing person + a found report without wristband
        JsonNode missingCase = post("/api/reports/missing", fam, "{\"eventId\":1,\"personName\":\"Lost Uncle\",\"approxAge\":66,\"gender\":\"MALE\","
                + "\"clothingColor\":\"WHITE\",\"lastSeenLat\":19.8100,\"lastSeenLng\":85.8260,\"lastSeenAt\":\"" + minutesAgo(40) + "\"}", 201);
        long caseId = missingCase.get("id").asLong();
        long missingId = missingCase.get("missing").get("id").asLong();
        String volunteer = token(VOLUNTEER);
        JsonNode foundCase = post("/api/reports/found", volunteer, "{\"eventId\":1,\"approxAge\":65,\"gender\":\"MALE\",\"clothingColor\":\"WHITE\","
                + "\"foundLat\":19.8105,\"foundLng\":85.8265,\"safeNotes\":\"confused, speaks Odia\"}", 201);
        long foundId = foundCase.get("found").get("id").asLong();
        assertThat(foundCase.get("assignedDeskName").asText()).isEqualTo("Medical Camp Help Desk");   // nearest desk

        String desk = token(DESK1);
        mvc.perform(get("/api/reports/missing/" + missingId + "/matches").header("Authorization", desk))
                .andExpect(jsonPath("$[?(@.foundReportId == " + foundId + ")].score", hasItem(100)));
        post("/api/cases/" + caseId + "/confirm-match", desk, "{\"foundReportId\":" + foundId + "}", 200);

        // desk realises it is the wrong person: undo
        JsonNode undone = post("/api/cases/" + caseId + "/reject-match", desk, "{\"note\":\"Family says this is not him\"}", 200);
        assertThat(undone.get("status").asText()).isEqualTo("REPORTED");
        assertThat(undone.has("found")).isFalse();
        mvc.perform(get("/api/cases/" + foundCase.get("id").asLong()).header("Authorization", desk))
                .andExpect(jsonPath("$.status").value("REPORTED"));                   // found-side case reopened
        mvc.perform(get("/api/reports/missing/" + missingId + "/matches").header("Authorization", desk))
                .andExpect(jsonPath("$[*].foundReportId", hasItem((int) foundId))); // available again

        // reports lists
        mvc.perform(get("/api/reports/missing").header("Authorization", fam)).andExpect(jsonPath("$.content[*].personName", hasItem("Lost Uncle")));
        mvc.perform(get("/api/reports/found").header("Authorization", desk)).andExpect(status().isOk());
        mvc.perform(get("/api/reports/found").header("Authorization", fam)).andExpect(status().isForbidden());

        // police assign + close the found-side case; family withdraws its own report
        String police = token(POLICE);
        post("/api/cases/" + foundCase.get("id").asLong() + "/assign", police, "{\"deskId\":2,\"officerId\":2}", 200);
        post("/api/cases/" + foundCase.get("id").asLong() + "/assign", police, "{\"officerId\":5}", 422);   // not police
        post("/api/cases/" + foundCase.get("id").asLong() + "/close", police, "{\"note\":\"Handed to family directly by police\"}", 200);
        post("/api/cases/" + caseId + "/close", token(FAMILY1), "{\"note\":\"not mine\"}", 404);             // other family
        JsonNode closed = post("/api/cases/" + caseId + "/close", fam, "{\"note\":\"He came home by himself\"}", 200);
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        mvc.perform(get("/api/cases/" + caseId + "/timeline").header("Authorization", fam))
                .andExpect(jsonPath("$[*].status", hasItems("REPORTED", "MATCHED", "CLOSED")));
        mvc.perform(get("/api/cases").param("status", "CLOSED").param("from", "2026-01-01").header("Authorization", police))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) caseId)));
    }

    @Test
    void validationAndBusinessRules() throws Exception {
        String fam = token(FAMILY1);
        post("/api/reports/missing", fam, "{\"eventId\":1,\"approxAge\":5,\"gender\":\"FEMALE\",\"clothingColor\":\"RED\",\"lastSeenLat\":19.8,"
                + "\"lastSeenLng\":85.8,\"lastSeenAt\":\"" + minutesAgo(5) + "\"}", 422);                      // name required (no member)
        post("/api/reports/missing", fam, "{\"eventId\":2,\"personName\":\"X\",\"approxAge\":5,\"gender\":\"FEMALE\",\"clothingColor\":\"RED\","
                + "\"lastSeenLat\":19.8,\"lastSeenLng\":85.8,\"lastSeenAt\":\"" + minutesAgo(5) + "\"}", 422);  // inactive event
        post("/api/reports/missing", fam, "{\"eventId\":1,\"memberId\":3,\"approxAge\":9,\"gender\":\"MALE\",\"clothingColor\":\"RED\","
                + "\"lastSeenLat\":19.8,\"lastSeenLng\":85.8,\"lastSeenAt\":\"" + minutesAgo(5) + "\"}", 404);  // someone else's member
        post("/api/reports/found", token(DESK1), "{\"eventId\":1,\"deskId\":99,\"approxAge\":5,\"gender\":\"MALE\",\"clothingColor\":\"RED\","
                + "\"foundLat\":19.8,\"foundLng\":85.8}", 404);
        post("/api/auth/register", "", "{\"fullName\":\"A\",\"email\":\"family1@milansetu.demo\",\"phone\":\"9876543210\",\"password\":\"Password123\"}", 422);
        post("/api/auth/register", "", "{\"fullName\":\"A\",\"email\":\"x@y.z\",\"phone\":\"12345\",\"password\":\"short\"}", 400);
    }
}

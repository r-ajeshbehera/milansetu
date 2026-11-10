package com.rajesh.milansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Two desks submit the correct OTP for the same case at the same moment: exactly one handover may succeed. */
class ConcurrentHandoverIT extends IntegrationTestBase {

    @Test
    void onlyOneDeskCompletesTheHandover() throws Exception {
        String volunteer = token(VOLUNTEER);
        JsonNode scan = read(mvc.perform(post("/api/scan/" + BHAGABAN_TOKEN).header("Authorization", volunteer))
                .andReturn().getResponse().getContentAsString());
        long caseId = scan.get("caseId").asLong();
        String desk1 = token(DESK1);
        String desk2 = token(DESK2);
        mvc.perform(post("/api/cases/" + caseId + "/confirm-match").header("Authorization", desk1).contentType(MediaType.APPLICATION_JSON).content("{}"));
        mvc.perform(post("/api/cases/" + caseId + "/arrived").header("Authorization", desk1));
        String otp = latestOtpFor(6L);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String desk : List.of(desk1, desk2)) {
            results.add(pool.submit(() -> {
                go.await();
                return mvc.perform(post("/api/cases/" + caseId + "/handover").header("Authorization", desk)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + otp + "\"}")).andReturn().getResponse().getStatus();
            }));
        }
        go.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }
}

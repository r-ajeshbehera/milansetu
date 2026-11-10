package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.dto.response.MatchSuggestion;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.service.MatchingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rajesh.milansetu.unit.TestData.T0;
import static org.assertj.core.api.Assertions.assertThat;

class MatchingRankTest {
    private final MissingReport missing = TestData.missing(Gender.MALE, 9, ClothingColor.YELLOW, 19.806, 85.820, T0);

    @Test void ranksHighestScoreFirstAndDropsBelowFifty() {
        FoundReport strong = TestData.found(1L, Gender.MALE, 10, ClothingColor.YELLOW, 19.806, 85.820, T0.plusMinutes(30)); // 100
        FoundReport medium = TestData.found(2L, Gender.MALE, 12, ClothingColor.YELLOW, 19.820, 85.820, T0.plusMinutes(200)); // 25+12+20+10+5=72
        FoundReport weak = TestData.found(3L, Gender.MALE, 20, ClothingColor.BLUE, 19.900, 85.820, T0.plusHours(10));     // 25
        FoundReport girl = TestData.found(4L, Gender.FEMALE, 9, ClothingColor.YELLOW, 19.806, 85.820, T0);                 // filtered
        List<MatchSuggestion> result = MatchingService.rank(missing, List.of(weak, girl, medium, strong));
        assertThat(result).extracting(MatchSuggestion::foundReportId).containsExactly(1L, 2L);
        assertThat(result).extracting(MatchSuggestion::score).containsExactly(100, 72);
    }

    @Test void onlyOpenReportsAreConsidered() {
        FoundReport matched = TestData.found(1L, Gender.MALE, 9, ClothingColor.YELLOW, 19.806, 85.820, T0);
        matched.setStatus(ReportStatus.MATCHED);
        assertThat(MatchingService.rank(missing, List.of(matched))).isEmpty();
    }

    @Test void tieBrokenByDistance() {
        FoundReport far = TestData.found(1L, Gender.MALE, 9, ClothingColor.YELLOW, 19.8135, 85.820, T0.plusMinutes(10)); // ~0.83 km
        FoundReport near = TestData.found(2L, Gender.MALE, 9, ClothingColor.YELLOW, 19.8070, 85.820, T0.plusMinutes(10)); // ~0.11 km
        assertThat(MatchingService.rank(missing, List.of(far, near))).extracting(MatchSuggestion::foundReportId).containsExactly(2L, 1L);
    }
}

package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.entity.ClothingColor;
import com.rajesh.milansetu.entity.Gender;
import com.rajesh.milansetu.service.MatchScorer;
import com.rajesh.milansetu.service.MatchScorer.Person;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** One test per scoring boundary. Base point: Puri, 10:00. 0.009° latitude ≈ 1.0 km. */
class MatchScorerTest {
    private static final double LAT = 19.8000, LNG = 85.8200;
    private static final LocalDateTime T = TestData.T0;
    private final Person missing = new Person(Gender.FEMALE, 6, ClothingColor.RED, LAT, LNG, T);

    private int score(Gender g, int age, ClothingColor c, double latOffset, long minutesLater) {
        return MatchScorer.score(missing, new Person(g, age, c, LAT + latOffset, LNG, T.plusMinutes(minutesLater)))
                .map(MatchScorer.Result::score).orElse(-1);
    }

    @Test void perfectMatchScores100() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, 30)).isEqualTo(100); }
    @Test void differentGenderIsNoMatch() { assertThat(MatchScorer.score(missing, new Person(Gender.MALE, 6, ClothingColor.RED, LAT, LNG, T))).isEmpty(); }
    @Test void ageDiffTwoGives25() { assertThat(score(Gender.FEMALE, 8, ClothingColor.RED, 0, 0)).isEqualTo(100); }
    @Test void ageDiffThreeGives12() { assertThat(score(Gender.FEMALE, 9, ClothingColor.RED, 0, 0)).isEqualTo(87); }
    @Test void ageDiffFiveGives12() { assertThat(score(Gender.FEMALE, 11, ClothingColor.RED, 0, 0)).isEqualTo(87); }
    @Test void ageDiffSixGivesZero() { assertThat(score(Gender.FEMALE, 12, ClothingColor.RED, 0, 0)).isEqualTo(75); }
    @Test void differentClothingLoses20() { assertThat(score(Gender.FEMALE, 6, ClothingColor.BLUE, 0, 0)).isEqualTo(80); }
    @Test void justUnderOneKmGives20() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0.0089, 0)).isEqualTo(100); }
    @Test void justOverOneKmGives10() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0.0091, 0)).isEqualTo(90); }
    @Test void justUnderThreeKmGives10() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0.0269, 0)).isEqualTo(90); }
    @Test void overThreeKmGivesZero() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0.0275, 0)).isEqualTo(80); }
    @Test void exactlyTwoHoursGives10() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, 120)).isEqualTo(100); }
    @Test void twoHoursOneMinuteGives5() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, 121)).isEqualTo(95); }
    @Test void exactlySixHoursGives5() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, 360)).isEqualTo(95); }
    @Test void overSixHoursGivesZero() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, 361)).isEqualTo(90); }
    @Test void foundBeforeLastSeenGivesZeroTimePoints() { assertThat(score(Gender.FEMALE, 6, ClothingColor.RED, 0, -30)).isEqualTo(90); }

    @Test void acrossMidnightUsesFullTimestamps() {
        Person late = new Person(Gender.FEMALE, 6, ClothingColor.RED, LAT, LNG, LocalDateTime.of(2027, 7, 7, 23, 30));
        Person nextDay = new Person(Gender.FEMALE, 6, ClothingColor.RED, LAT, LNG, LocalDateTime.of(2027, 7, 8, 0, 30));
        assertThat(MatchScorer.score(late, nextDay).orElseThrow().minutesApart()).isEqualTo(60L);
        assertThat(MatchScorer.score(late, nextDay).orElseThrow().score()).isEqualTo(100);
    }

    @Test void thresholdIsFifty() {
        assertThat(MatchScorer.isSuggestion(50)).isTrue();
        assertThat(MatchScorer.isSuggestion(49)).isFalse();
    }

    @Test void factorsExplainTheScore() {
        MatchScorer.Result r = MatchScorer.score(missing, new Person(Gender.FEMALE, 9, ClothingColor.BLUE, LAT + 0.02, LNG, T.plusMinutes(200))).orElseThrow();
        assertThat(r.score()).isEqualTo(25 + 12 + 0 + 10 + 5);
        assertThat(r.factors()).extracting(f -> f.factor()).containsExactly("GENDER", "AGE", "CLOTHING", "DISTANCE", "TIME");
        assertThat(r.factors()).extracting(f -> f.points()).containsExactly(25, 12, 0, 10, 5);
    }
}

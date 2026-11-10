package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.entity.HandoverOtp;
import com.rajesh.milansetu.entity.HandoverOtp.Result;
import com.rajesh.milansetu.entity.OtpStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class HandoverOtpTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2027, 7, 7, 12, 0);
    private static final Predicate<String> CORRECT_IS_123456 = "123456"::equals;   // stands in for BCrypt.matches

    private HandoverOtp otp() {
        return new HandoverOtp(1L, "hash", NOW.plusMinutes(10), 3L);
    }

    @Test void correctCodeSucceedsAndMarksUsed() {
        HandoverOtp o = otp();
        assertThat(o.verify("123456", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.SUCCESS);
        assertThat(o.getStatus()).isEqualTo(OtpStatus.USED);
        assertThat(o.getUsedAt()).isEqualTo(NOW);
    }

    @Test void wrongCodeCountsAttempts() {
        HandoverOtp o = otp();
        assertThat(o.verify("000000", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.WRONG_CODE);
        assertThat(o.getAttempts()).isEqualTo(1);
        assertThat(o.attemptsLeft(5)).isEqualTo(4);
    }

    @Test void fifthWrongAttemptLocks() {
        HandoverOtp o = otp();
        for (int i = 0; i < 4; i++) o.verify("000000", NOW, CORRECT_IS_123456, 5);
        assertThat(o.verify("000000", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.LOCKED_NOW);
        assertThat(o.getStatus()).isEqualTo(OtpStatus.LOCKED);
        // even the correct code is refused once locked
        assertThat(o.verify("123456", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.LOCKED);
    }

    @Test void expiredAfterTenMinutes() {
        HandoverOtp o = otp();
        assertThat(o.verify("123456", NOW.plusMinutes(10).plusSeconds(1), CORRECT_IS_123456, 5)).isEqualTo(Result.EXPIRED);
        assertThat(o.getStatus()).isEqualTo(OtpStatus.EXPIRED);
    }

    @Test void stillValidAtExactlyTenMinutes() {
        assertThat(otp().verify("123456", NOW.plusMinutes(10), CORRECT_IS_123456, 5)).isEqualTo(Result.SUCCESS);
    }

    @Test void usedCodeIsRejected() {
        HandoverOtp o = otp();
        o.verify("123456", NOW, CORRECT_IS_123456, 5);
        assertThat(o.verify("123456", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.ALREADY_USED);
    }

    @Test void supersededCodeIsRejected() {
        HandoverOtp o = otp();
        o.supersede();
        assertThat(o.verify("123456", NOW, CORRECT_IS_123456, 5)).isEqualTo(Result.NOT_ACTIVE);
    }

    @Test void malformedInputCountsAsWrongWithoutCallingMatcher() {
        HandoverOtp o = otp();
        assertThat(o.verify("12ab56", NOW, code -> { throw new AssertionError("matcher must not run"); }, 5)).isEqualTo(Result.WRONG_CODE);
    }
}

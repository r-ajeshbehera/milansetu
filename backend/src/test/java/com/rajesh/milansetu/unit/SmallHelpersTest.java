package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.entity.HelpDesk;
import com.rajesh.milansetu.entity.Member;
import com.rajesh.milansetu.entity.Gender;
import com.rajesh.milansetu.service.DeskLocator;
import com.rajesh.milansetu.service.GeoUtils;
import com.rajesh.milansetu.service.OtpGenerator;
import com.rajesh.milansetu.service.QrTokens;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SmallHelpersTest {

    @Test void haversineKnownDistance() {
        // Singhadwara -> Gundicha Temple (approx. 1.6 km along Grand Road)
        assertThat(GeoUtils.haversineKm(19.8049, 85.8182, 19.8135, 85.8312)).isCloseTo(1.66, within(0.05));
    }

    @Test void haversineZero() {
        assertThat(GeoUtils.haversineKm(19.8, 85.8, 19.8, 85.8)).isEqualTo(0.0);
    }

    @Test void otpIsSixDigitsWithLeadingZeros() {
        Random r = new Random() { @Override public int nextInt(int bound) { return 4512; } };
        assertThat(OtpGenerator.generate(r)).isEqualTo("004512");
        for (int i = 0; i < 200; i++) assertThat(OtpGenerator.generate().length()).isEqualTo(6);
    }

    @Test void qrTokenParsing() {
        String uuid = "3f2a9c1e-7b4d-4e8a-9c21-5d6f7a8b9c01";
        assertThat(QrTokens.parse("MILANSETU:" + uuid)).contains(uuid);
        assertThat(QrTokens.parse("  " + uuid + " ")).contains(uuid);
        assertThat(QrTokens.parse("milansetu:" + uuid)).contains(uuid);
        assertThat(QrTokens.parse("42")).isEmpty();
        assertThat(QrTokens.parse("MILANSETU:not-a-uuid")).isEmpty();
        assertThat(QrTokens.parse(null)).isEmpty();
    }

    @Test void qrTokenIsRandomAndNotTheId() {
        Member a = new Member(TestData.FAMILY, TestData.EVENT, "Riya Mohanty", 6, Gender.FEMALE, null);
        Member b = new Member(TestData.FAMILY, TestData.EVENT, "Riya Mohanty", 6, Gender.FEMALE, null);
        assertThat(a.getQrToken().equals(b.getQrToken())).isFalse();
        assertThat(QrTokens.parse(a.getQrToken())).contains(a.getQrToken());
        String old = a.getQrToken();
        a.regenerateToken();
        assertThat(a.getQrToken().equals(old)).isFalse();
        assertThat(a.firstName()).isEqualTo("Riya");
    }

    @Test void nearestActiveDesk() {
        HelpDesk singhadwara = TestData.desk(1L, "Singhadwara", 19.8049, 85.8182);
        HelpDesk gundicha = TestData.desk(2L, "Gundicha", 19.8135, 85.8312);
        HelpDesk medical = TestData.desk(3L, "Medical", 19.8090, 85.8245);
        assertThat(DeskLocator.nearest(List.of(singhadwara, gundicha, medical), 19.8130, 85.8300).orElseThrow().desk().getName()).isEqualTo("Gundicha");
        gundicha.setActive(false);
        assertThat(DeskLocator.nearest(List.of(singhadwara, gundicha, medical), 19.8130, 85.8300).orElseThrow().desk().getName()).isEqualTo("Medical");
    }
}

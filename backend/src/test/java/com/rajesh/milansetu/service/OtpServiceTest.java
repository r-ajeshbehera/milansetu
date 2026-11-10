package com.rajesh.milansetu.service;

import com.rajesh.milansetu.config.AppProperties;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.InvalidStateException;
import com.rajesh.milansetu.exception.OtpRejectedException;
import com.rajesh.milansetu.notification.NotificationService;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import com.rajesh.milansetu.unit.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {
    private final HandoverOtpRepository otps = mock(HandoverOtpRepository.class);
    private final CaseFileRepository cases = mock(CaseFileRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);   // fast for tests
    private final AppUserPrincipal desk = new AppUserPrincipal(3L, "d@x", "h", Role.DESK, true);
    private OtpService service;
    private CaseFile c;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null, null, new AppProperties.Otp(10, 5, 3, 30), null, null);
        service = new OtpService(otps, cases, mock(CaseEventRepository.class), mock(UserRepository.class), notifications,
                encoder, mock(AccessPolicy.class), new CaseMapper(), mock(AuditService.class), props);
        c = CaseFile.forMissing(TestData.missing(Gender.MALE, 9, ClothingColor.RED, 19.8, 85.8, TestData.T0));
        c.setIdForTest(7L);
        when(cases.findWithDetails(7L)).thenReturn(Optional.of(c));
        when(otps.save(any(HandoverOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notifications.channel()).thenReturn("MOCK");
    }

    @Test
    void issuedCodeIsHashedAndSentOnlyToGuardian() {
        c.setStatusForTest(CaseStatus.MATCHED);
        var res = service.issue(7L, desk);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HandoverOtp> saved = ArgumentCaptor.forClass(HandoverOtp.class);
        verify(notifications).sendOtp(eq(TestData.FAMILY), eq(7L), code.capture(), any());
        verify(otps).save(saved.capture());
        assertThat(code.getValue()).matches("\\d{6}");
        assertThat(saved.getValue().getOtpHash()).startsWith("$2a$");
        assertThat(encoder.matches(code.getValue(), saved.getValue().getOtpHash())).isTrue();
        assertThat(res.toString()).doesNotContain(code.getValue());
        assertThat(saved.getValue().getExpiresAt()).isBetween(LocalDateTime.now().plusMinutes(9), LocalDateTime.now().plusMinutes(11));
    }

    @Test
    void cannotIssueForReportedCase() {
        assertThatThrownBy(() -> service.issue(7L, desk)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void handoverRequiresAtDesk() {
        c.setStatusForTest(CaseStatus.MATCHED);
        assertThatThrownBy(() -> service.verifyAndHandover(7L, "123456", desk)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void wrongCodeIsRejectedAndCounted() {
        c.setStatusForTest(CaseStatus.AT_DESK);
        HandoverOtp otp = new HandoverOtp(7L, encoder.encode("123456"), LocalDateTime.now().plusMinutes(5), 3L);
        when(otps.findFirstByCaseIdOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(otp));
        assertThatThrownBy(() -> service.verifyAndHandover(7L, "999999", desk)).isInstanceOf(OtpRejectedException.class);
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(c.getStatus()).isEqualTo(CaseStatus.AT_DESK);
    }

    @Test
    void correctCodeReunites() {
        c.setStatusForTest(CaseStatus.AT_DESK);
        HandoverOtp otp = new HandoverOtp(7L, encoder.encode("123456"), LocalDateTime.now().plusMinutes(5), 3L);
        when(otps.findFirstByCaseIdOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(otp));
        var res = service.verifyAndHandover(7L, "123456", desk);
        assertThat(res.status()).isEqualTo(CaseStatus.REUNITED);
        assertThat(otp.getStatus()).isEqualTo(OtpStatus.USED);
        assertThat(c.getMissingReport().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(cases).saveAndFlush(c);
    }
}

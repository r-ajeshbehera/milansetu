package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseStatusTest {

    private CaseFile newCase() {
        CaseFile c = CaseFile.forMissing(TestData.missing(Gender.MALE, 9, ClothingColor.RED, 19.8, 85.8, TestData.T0));
        c.setIdForTest(10L);
        return c;
    }

    @Test void validForwardPath() {
        CaseFile c = newCase();
        c.moveTo(CaseStatus.MATCHED);
        c.moveTo(CaseStatus.AT_DESK);
        c.markReunited();
        c.moveTo(CaseStatus.CLOSED);
        assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
        assertThat(c.getReunitedAt()).isNotNull();
    }

    @Test void reunitedCannotBeSetThroughGenericTransition() {
        CaseFile c = newCase();
        c.moveTo(CaseStatus.MATCHED);
        c.moveTo(CaseStatus.AT_DESK);
        assertThatThrownBy(() -> c.moveTo(CaseStatus.REUNITED)).isInstanceOf(InvalidStateException.class);
        assertThat(CaseStatus.AT_DESK.canMoveTo(CaseStatus.REUNITED)).isFalse();
    }

    @Test void reunitedRequiresAtDesk() {
        CaseFile c = newCase();
        c.moveTo(CaseStatus.MATCHED);
        assertThatThrownBy(c::markReunited).isInstanceOf(InvalidStateException.class);
    }

    @Test void cannotSkipMatching() {
        assertThatThrownBy(() -> newCase().moveTo(CaseStatus.AT_DESK)).isInstanceOf(InvalidStateException.class);
    }

    @Test void closedIsFinal() {
        CaseFile c = newCase();
        c.moveTo(CaseStatus.CLOSED);
        assertThatThrownBy(() -> c.moveTo(CaseStatus.REPORTED)).isInstanceOf(InvalidStateException.class);
    }

    @Test void wrongMatchCanBeUndone() {
        CaseFile c = newCase();
        c.moveTo(CaseStatus.MATCHED);
        c.moveTo(CaseStatus.REPORTED);
        assertThat(c.getMatchedAt()).isNull();
    }

    @Test void guardianIsMissingReportFiler() {
        assertThat(newCase().guardian()).isSameAs(TestData.FAMILY);
    }
}

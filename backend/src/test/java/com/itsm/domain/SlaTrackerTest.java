package com.itsm.domain;

import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.incident.SlaTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.itsm.TestFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;

class SlaTrackerTest {

    private SlaTracker tracker() {
        SlaTracker t = new SlaTracker();
        t.start(T0, Duration.ofMinutes(5), Duration.ofMinutes(30), 75);
        return t;
    }

    @Test
    @DisplayName("TC-11 Deadlines are computed from the start time and policy")
    void deadlines() {
        SlaTracker t = tracker();
        assertThat(t.getResponseDueAt()).isEqualTo(T0.plusSeconds(300));
        assertThat(t.getResolutionDueAt()).isEqualTo(T0.plusSeconds(1800));
    }

    @Test
    @DisplayName("TC-12 Boundary: exactly at the deadline is not a breach, one second later is")
    void breachBoundary() {
        SlaTracker t = tracker();
        assertThat(t.detectBreaches(T0.plusSeconds(300))).isFalse();
        assertThat(t.detectBreaches(T0.plusSeconds(301))).isTrue();
        assertThat(t.isResponseBreached()).isTrue();
        // latched: detecting again does not report a "new" breach
        assertThat(t.detectBreaches(T0.plusSeconds(400))).isFalse();
    }

    @Test
    @DisplayName("TC-13 Warning is raised once the threshold (75%) is crossed")
    void warningThreshold() {
        SlaTracker t = tracker();
        assertThat(t.shouldWarn(T0.plusSeconds(224))).isFalse(); // 74.7%
        assertThat(t.shouldWarn(T0.plusSeconds(225))).isTrue();  // 75%
        assertThat(t.statusAt(T0.plusSeconds(230), IncidentStatus.ASSIGNED)).isEqualTo(SlaStatus.AT_RISK);
        t.markWarningRaised();
        assertThat(t.shouldWarn(T0.plusSeconds(250))).isFalse();
    }

    @Test
    @DisplayName("Resolution inside the window is MET; late resolution is BREACHED")
    void metVsBreached() {
        SlaTracker ok = tracker();
        ok.markResponded(T0.plusSeconds(60));
        ok.markResolved(T0.plusSeconds(900));
        assertThat(ok.statusAt(T0.plusSeconds(1000), IncidentStatus.RESOLVED)).isEqualTo(SlaStatus.MET);
        assertThat(ok.resolutionTime()).isEqualTo(Duration.ofSeconds(900));

        SlaTracker late = tracker();
        late.markResponded(T0.plusSeconds(60));
        late.markResolved(T0.plusSeconds(1900));
        assertThat(late.statusAt(T0.plusSeconds(2000), IncidentStatus.RESOLVED)).isEqualTo(SlaStatus.BREACHED);
    }
}

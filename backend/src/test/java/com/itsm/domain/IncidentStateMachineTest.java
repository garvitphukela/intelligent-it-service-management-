package com.itsm.domain;

import com.itsm.TestFixtures;
import com.itsm.domain.incident.*;
import com.itsm.domain.user.Technician;
import com.itsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.itsm.TestFixtures.T0;
import static org.assertj.core.api.Assertions.*;

/** State pattern: legal lifecycle and rejection of inappropriate transitions. */
class IncidentStateMachineTest {

    private Incident incident;
    private Technician tech;

    @BeforeEach
    void setUp() {
        incident = TestFixtures.classifiedIncident(1, Priority.P2, TestFixtures.service(1, "Payment", 5), TestFixtures.category(1, "APPLICATION"));
        tech = TestFixtures.technician(10, Set.of("APPLICATION"), 5, 5);
    }

    @Test
    @DisplayName("TC-01 Happy path: CLASSIFIED -> ASSIGNED -> ... -> CLOSED")
    void fullLifecycle() {
        incident.assignTo(tech, T0.plusSeconds(10));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        incident.acknowledge(T0.plusSeconds(60));
        incident.startWork(T0.plusSeconds(120));
        incident.resolve(new Resolution("Restarted pods", "leak", "rollback", "Tech", T0.plusSeconds(600)), T0.plusSeconds(600));
        incident.verify(T0.plusSeconds(700));
        incident.close("ok", "Tech", T0.plusSeconds(800));

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getSla().getRespondedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(incident.getSla().isBreached()).isFalse();
        assertThat(incident.getClosedBy()).isEqualTo("Tech");
    }

    @Test
    @DisplayName("TC-02 NEW -> CLOSED is rejected")
    void newCannotBeClosed() {
        Incident fresh = new Incident("t1234", "description long enough", TestFixtures.employee(1), TestFixtures.service(2, "Net", 3),
                Impact.LOW, Urgency.LOW, 1, T0);
        assertThat(fresh.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThatThrownBy(() -> fresh.close("x", "y", T0))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("NEW");
    }

    @Test
    @DisplayName("TC-03 Cannot resolve an incident that was never acknowledged")
    void resolveRequiresWork() {
        incident.assignTo(tech, T0);
        assertThatThrownBy(() -> incident.resolve(new Resolution("x", null, null, "t", T0), T0))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC-04 Closed is terminal: every action is rejected")
    void closedIsTerminal() {
        fullLifecycle();
        assertThat(incident.allowedNextStatuses()).isEmpty();
        assertThatThrownBy(() -> incident.reopen(T0)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> incident.assignTo(tech, T0)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> incident.cancel("r", T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC-05 Awaiting info pauses the resolution SLA and extends the deadline")
    void awaitingInfoPausesSla() {
        incident.assignTo(tech, T0);
        incident.acknowledge(T0.plusSeconds(30));
        incident.startWork(T0.plusSeconds(60));
        var dueBefore = incident.getSla().getResolutionDueAt();
        incident.requestInfo(T0.plusSeconds(120));
        incident.refreshSlaStatus(T0.plusSeconds(500));
        assertThat(incident.getSlaStatus()).isEqualTo(SlaStatus.PAUSED);
        incident.provideInfo(T0.plusSeconds(720)); // paused for 600s
        assertThat(incident.getSla().getResolutionDueAt()).isEqualTo(dueBefore.plusSeconds(600));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("TC-06 Reopening a resolved incident clears the resolution")
    void reopenClearsResolution() {
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        incident.startWork(T0);
        incident.resolve(new Resolution("fix", null, null, "t", T0.plusSeconds(60)), T0.plusSeconds(60));
        incident.reopen(T0.plusSeconds(90));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getResolution()).isNull();
        assertThat(incident.getSla().getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("TC-07 Cancel allowed before acknowledgement, rejected during work, reason required")
    void cancelRules() {
        assertThatThrownBy(() -> incident.cancel(" ", T0)).isInstanceOf(IllegalArgumentException.class);
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        assertThatThrownBy(() -> incident.cancel("dup", T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC-08 Re-assignment returns the incident to ASSIGNED so the new owner must acknowledge")
    void reassignmentResetsToAssigned() {
        incident.assignTo(tech, T0);
        incident.acknowledge(T0);
        Technician other = TestFixtures.technician(11, Set.of("APPLICATION"), 2, 5);
        incident.assignTo(other, T0.plusSeconds(5));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(incident.getAssignedTechnician()).isEqualTo(other);
        assertThat(incident.can(IncidentAction.ACKNOWLEDGE)).isTrue();
    }

    @Test
    @DisplayName("TC-09 Feedback only after resolution and rating must be 1..5")
    void feedbackRules() {
        assertThatThrownBy(() -> incident.submitFeedback(new Feedback(5, null, T0))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new Feedback(6, null, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Feedback(0, null, T0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC-10 An incident cannot be a duplicate of itself or of another duplicate")
    void duplicateLinkRules() {
        assertThatThrownBy(() -> incident.linkAsDuplicateOf(incident)).isInstanceOf(IllegalArgumentException.class);
        Incident master = TestFixtures.classifiedIncident(2, Priority.P2, incident.getService(), incident.getCategory());
        Incident grand = TestFixtures.classifiedIncident(3, Priority.P2, incident.getService(), incident.getCategory());
        master.linkAsDuplicateOf(grand);
        assertThatThrownBy(() -> incident.linkAsDuplicateOf(master)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Incident number <-> id conversion")
    void numberConversion() {
        assertThat(incident.getNumber()).isEqualTo("INC1001");
        assertThat(Incident.idFromNumber("INC1001")).isEqualTo(1L);
        assertThat(Incident.idFromNumber("inc-1024")).isEqualTo(24L);
        assertThat(Incident.idFromNumber("abc")).isNull();
    }
}

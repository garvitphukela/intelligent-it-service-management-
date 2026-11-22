package com.itsm.strategy;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.priority.MatrixPriorityStrategy;
import com.itsm.priority.PriorityInput;
import com.itsm.priority.PriorityStrategy;
import com.itsm.priority.WeightedScoringPriorityStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class PriorityStrategyTest {

    private final PriorityStrategy weighted = new WeightedScoringPriorityStrategy();
    private final PriorityStrategy matrix = new MatrixPriorityStrategy();

    @Test
    @DisplayName("TC-14 Spec scenario: payment app unavailable for everyone is P1")
    void paymentOutageIsP1() {
        var r = weighted.calculate(new PriorityInput(Impact.EXTENSIVE, Urgency.CRITICAL, 5, 3000, 3000, "APPLICATION"));
        assertThat(r.priority()).isEqualTo(Priority.P1);
        assertThat(r.score()).isEqualTo(120.0); // 4*4*5 = 80 x1.5 users
        assertThat(r.rationale()).contains("Impact EXTENSIVE").contains("P1");
    }

    @Test
    @DisplayName("TC-15 Boundary: minimum inputs give P4")
    void minimumIsP4() {
        assertThat(weighted.calculate(new PriorityInput(Impact.LOW, Urgency.LOW, 1, 1, 1000, null)).priority()).isEqualTo(Priority.P4);
    }

    @ParameterizedTest(name = "impact={0} urgency={1} crit={2} -> {3}")
    @DisplayName("TC-16 Threshold boundaries of the weighted algorithm")
    @CsvSource({
            "HIGH, HIGH, 4, P1",      // 36 exactly -> P1
            "HIGH, MEDIUM, 5, P2",    // 30
            "MEDIUM, MEDIUM, 4, P3",  // 16 -> just below P2 (18)
            "MEDIUM, HIGH, 3, P2",    // 18 exactly -> P2
            "LOW, MEDIUM, 4, P3",     // 8 exactly -> P3
            "LOW, HIGH, 2, P4"        // 6
    })
    void thresholds(Impact impact, Urgency urgency, int crit, Priority expected) {
        assertThat(weighted.calculate(new PriorityInput(impact, urgency, crit, 1, 1000, null)).priority()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Affected users and security category raise the score")
    void boosts() {
        double base = weighted.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 3, 1, 1000, "NETWORK")).score();
        double many = weighted.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 3, 600, 1000, "NETWORK")).score();
        double sec = weighted.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 3, 1, 1000, "SECURITY")).score();
        assertThat(many).isGreaterThan(base);
        assertThat(sec).isGreaterThan(base);
    }

    @Test
    @DisplayName("ITIL matrix is raised for mission-critical and lowered for non-critical services")
    void matrixAdjustments() {
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 3, 1, 10, null)).priority()).isEqualTo(Priority.P3);
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 5, 1, 10, null)).priority()).isEqualTo(Priority.P2);
        assertThat(matrix.calculate(new PriorityInput(Impact.MEDIUM, Urgency.MEDIUM, 1, 1, 10, null)).priority()).isEqualTo(Priority.P4);
        assertThat(matrix.calculate(new PriorityInput(Impact.EXTENSIVE, Urgency.CRITICAL, 5, 1, 10, null)).priority()).isEqualTo(Priority.P1);
    }

    @Test
    @DisplayName("TC-17 Invalid input: criticality outside 1..5 or missing impact is rejected")
    void invalidInput() {
        assertThatThrownBy(() -> new PriorityInput(Impact.LOW, Urgency.LOW, 6, 1, 1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PriorityInput(null, Urgency.LOW, 3, 1, 1, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

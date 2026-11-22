package com.itsm.priority;

import com.itsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

/**
 * Classic ITIL impact/urgency matrix, then adjusted one level up for mission-critical
 * services (criticality 5) or one level down for low-criticality services (criticality 1).
 */
@Component
public class MatrixPriorityStrategy implements PriorityStrategy {

    public static final String KEY = "ITIL_MATRIX";

    //                                   urgency: LOW          MEDIUM       HIGH         CRITICAL
    private static final Priority[][] MATRIX = {
            /* impact LOW       */ {Priority.P4, Priority.P4, Priority.P3, Priority.P3},
            /* impact MEDIUM    */ {Priority.P4, Priority.P3, Priority.P3, Priority.P2},
            /* impact HIGH      */ {Priority.P3, Priority.P3, Priority.P2, Priority.P1},
            /* impact EXTENSIVE */ {Priority.P3, Priority.P2, Priority.P1, Priority.P1},
    };

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "ITIL matrix"; }
    @Override public String description() {
        return "Impact/Urgency lookup table, raised for mission-critical services and lowered for non-critical ones.";
    }

    @Override
    public PriorityResult calculate(PriorityInput in) {
        Priority base = MATRIX[in.impact().ordinal()][in.urgency().ordinal()];
        Priority adjusted = base;
        String adj = "no adjustment";
        if (in.businessCriticality() >= 5) {
            adjusted = base.raise();
            adj = "raised for mission-critical service";
        } else if (in.businessCriticality() <= 1) {
            adjusted = base.lower();
            adj = "lowered for non-critical service";
        }
        double score = (5 - adjusted.level()) * 25.0; // P1=100, P4=25 for display
        String rationale = "Matrix[" + in.impact() + "][" + in.urgency() + "] = " + base + ", " + adj + " -> " + adjusted;
        return new PriorityResult(adjusted, score, rationale);
    }
}

package com.itsm.priority;

import com.itsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Default algorithm:
 * <pre>
 *   base  = impact(1..4) x urgency(1..4) x businessCriticality(1..5)      range 1..80
 *   score = base x affectedUsersFactor(1.0..1.5) x categoryFactor(1.0..1.2)
 *   P1 >= 36,  P2 >= 18,  P3 >= 8,  otherwise P4
 * </pre>
 * Multiplying keeps the factors independent: a low-urgency issue on a critical
 * service stays moderate, while high impact AND high urgency on a critical service
 * escalates quickly. Affected-user count and security-sensitive categories nudge the
 * score upward without dominating it.
 */
@Component
public class WeightedScoringPriorityStrategy implements PriorityStrategy {

    public static final String KEY = "WEIGHTED";

    static final double P1_THRESHOLD = 36;
    static final double P2_THRESHOLD = 18;
    static final double P3_THRESHOLD = 8;

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Weighted scoring"; }
    @Override public String description() {
        return "Impact x Urgency x Business criticality, boosted by affected users and security-sensitive categories.";
    }

    @Override
    public PriorityResult calculate(PriorityInput in) {
        int base = in.impact().weight() * in.urgency().weight() * in.businessCriticality();
        double userFactor = affectedUsersFactor(in.affectedUsers(), in.serviceUserBase());
        double categoryFactor = categoryFactor(in.categoryName());
        double score = round(base * userFactor * categoryFactor);

        Priority p = score >= P1_THRESHOLD ? Priority.P1
                : score >= P2_THRESHOLD ? Priority.P2
                : score >= P3_THRESHOLD ? Priority.P3
                : Priority.P4;

        String rationale = String.format(Locale.ROOT,
                "Impact %s(%d) x Urgency %s(%d) x Criticality %d = %d; x%.2f affected users (%d) x%.2f category => %.1f -> %s",
                in.impact(), in.impact().weight(), in.urgency(), in.urgency().weight(), in.businessCriticality(), base,
                userFactor, in.affectedUsers(), categoryFactor, score, p);
        return new PriorityResult(p, score, rationale);
    }

    static double affectedUsersFactor(int affected, int userBase) {
        double ratio = (double) affected / userBase;
        if (affected >= 1000 || ratio >= 0.5) return 1.5;
        if (affected >= 100 || ratio >= 0.2) return 1.25;
        if (affected >= 10) return 1.1;
        return 1.0;
    }

    static double categoryFactor(String category) {
        if (category == null) return 1.0;
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "SECURITY" -> 1.2;
            case "AUTHENTICATION" -> 1.1;
            default -> 1.0;
        };
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}

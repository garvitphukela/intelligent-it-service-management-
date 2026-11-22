package com.itsm.assignment;

import com.itsm.domain.incident.Priority;
import com.itsm.domain.user.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Intelligent multi-factor recommendation. Weighted sum of:
 * <ul>
 *   <li>skill match with the category</li>
 *   <li>spare capacity</li>
 *   <li>years of experience (capped at 10)</li>
 *   <li>past incidents resolved on the same service (capped at 5)</li>
 * </ul>
 * For P1/P2 incidents experience matters more and load matters less.
 */
@Component
public class SmartRecommendationStrategy implements AssignmentStrategy {

    public static final String KEY = "SMART";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Smart recommendation"; }
    @Override public String description() {
        return "Weighs skills, workload, experience and history with the affected service; favours seniority for critical incidents.";
    }

    @Override
    public List<TechnicianScore> rank(AssignmentContext ctx) {
        Priority p = ctx.incident().getPriority();
        boolean critical = p == Priority.P1 || p == Priority.P2;
        double wSkill = 0.40, wLoad = critical ? 0.15 : 0.25, wExp = critical ? 0.25 : 0.15, wHist = 0.20;
        String skill = ctx.requiredSkill();

        return ctx.candidates().stream()
                .map(t -> {
                    double skillS = t.hasSkill(skill) ? 1.0 : 0.0;
                    double loadS = 1.0 - Math.min(1.0, ctx.loadRatio(t));
                    double expS = Math.min(10, t.getExperienceYears()) / 10.0;
                    double histS = Math.min(5, ctx.serviceExperienceOf(t)) / 5.0;
                    double total = wSkill * skillS + wLoad * loadS + wExp * expS + wHist * histS;
                    Map<String, Double> breakdown = new LinkedHashMap<>();
                    breakdown.put("skill", round(wSkill * skillS));
                    breakdown.put("workload", round(wLoad * loadS));
                    breakdown.put("experience", round(wExp * expS));
                    breakdown.put("serviceHistory", round(wHist * histS));
                    return new TechnicianScore(t, round(total), ctx.workloadOf(t), breakdown, explain(t, skillS, ctx));
                })
                .sorted(Comparator.comparingDouble(TechnicianScore::score).reversed()
                        .thenComparing(s -> s.technician().getId()))
                .toList();
    }

    private String explain(Technician t, double skill, AssignmentContext ctx) {
        return String.format(Locale.ROOT, "%s, %d yrs exp, %d/%d load, %d past fixes on %s",
                skill > 0 ? "Skill match" : "No skill match", t.getExperienceYears(), ctx.workloadOf(t), t.getMaxWorkload(),
                ctx.serviceExperienceOf(t), ctx.incident().getService().getName());
    }

    private static double round(double v) { return Math.round(v * 1000) / 1000.0; }
}

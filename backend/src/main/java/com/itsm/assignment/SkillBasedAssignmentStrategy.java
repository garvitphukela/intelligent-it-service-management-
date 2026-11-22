package com.itsm.assignment;

import com.itsm.domain.user.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Prefer technicians whose skills match the incident category; break ties by lowest load. */
@Component
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "SKILL_BASED";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Skill-based"; }
    @Override public String description() { return "Matches the category's required skill, then picks the least busy matching technician."; }

    @Override
    public List<TechnicianScore> rank(AssignmentContext ctx) {
        String skill = ctx.requiredSkill();
        return ctx.candidates().stream()
                .map(t -> score(t, skill, ctx))
                .sorted(Comparator.comparingDouble(TechnicianScore::score).reversed()
                        .thenComparing(s -> s.technician().getId()))
                .toList();
    }

    private TechnicianScore score(Technician t, String skill, AssignmentContext ctx) {
        double skillMatch = t.hasSkill(skill) ? 1.0 : 0.0;
        double spare = 1.0 - Math.min(1.0, ctx.loadRatio(t));
        double score = skillMatch * 0.8 + spare * 0.2;
        String reason = skillMatch > 0 ? "Has skill " + skill : "No " + skill + " skill (fallback)";
        return new TechnicianScore(t, score, ctx.workloadOf(t), Map.of("skill", skillMatch, "spareCapacity", spare), reason);
    }
}

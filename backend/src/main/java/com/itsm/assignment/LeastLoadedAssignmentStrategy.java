package com.itsm.assignment;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Balances work: chooses the technician with the most spare capacity (relative to their own max). */
@Component
public class LeastLoadedAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "LOAD_BASED";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Load-based"; }
    @Override public String description() { return "Assigns to whoever has the most spare capacity, regardless of skill."; }

    @Override
    public List<TechnicianScore> rank(AssignmentContext ctx) {
        return ctx.candidates().stream()
                .map(t -> {
                    double spare = 1.0 - Math.min(1.0, ctx.loadRatio(t));
                    return new TechnicianScore(t, spare, ctx.workloadOf(t), Map.of("spareCapacity", spare),
                            ctx.workloadOf(t) + "/" + t.getMaxWorkload() + " active incidents");
                })
                .sorted(Comparator.comparingDouble(TechnicianScore::score).reversed()
                        .thenComparing(s -> s.technician().getId()))
                .toList();
    }
}

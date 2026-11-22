package com.itsm.assignment;

import com.itsm.domain.user.Technician;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rotates through skill-matching technicians (or everyone if nobody matches) in id order.
 * Thread-safe: the rotation pointer is an {@link AtomicLong}.
 */
@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "ROUND_ROBIN";

    private final AtomicLong lastAssignedId = new AtomicLong(0);

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Round-robin"; }
    @Override public String description() { return "Takes turns among technicians with the right skill, for an even spread."; }

    @Override
    public List<TechnicianScore> rank(AssignmentContext ctx) {
        String skill = ctx.requiredSkill();
        List<Technician> pool = ctx.candidates().stream().filter(t -> t.hasSkill(skill)).toList();
        if (pool.isEmpty()) pool = ctx.candidates();
        List<Technician> sorted = new ArrayList<>(pool);
        sorted.sort(Comparator.comparing(Technician::getId));

        long last = lastAssignedId.get();
        int start = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId() > last) { start = i; break; }
            if (i == sorted.size() - 1) start = 0;
        }
        List<TechnicianScore> ranked = new ArrayList<>();
        for (int k = 0; k < sorted.size(); k++) {
            Technician t = sorted.get((start + k) % sorted.size());
            double score = 1.0 - (double) k / Math.max(1, sorted.size());
            ranked.add(new TechnicianScore(t, score, ctx.workloadOf(t), Map.of("turnOrder", (double) k), "Turn " + (k + 1) + " in rotation"));
        }
        return ranked;
    }

    @Override
    public java.util.Optional<TechnicianScore> select(AssignmentContext ctx) {
        var choice = rank(ctx).stream().findFirst();
        choice.ifPresent(s -> lastAssignedId.set(s.technician().getId()));
        return choice;
    }
}

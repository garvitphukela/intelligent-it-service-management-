package com.itsm.strategy;

import com.itsm.TestFixtures;
import com.itsm.assignment.*;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.user.Technician;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Strategy pattern: four interchangeable assignment algorithms behind one interface. */
class AssignmentStrategyTest {

    private Technician dbJunior, dbSenior, netGuy;
    private Incident dbIncident;

    @BeforeEach
    void setUp() {
        dbJunior = TestFixtures.technician(1, Set.of("DATABASE"), 1, 5);
        dbSenior = TestFixtures.technician(2, Set.of("DATABASE", "STORAGE"), 9, 5);
        netGuy = TestFixtures.technician(3, Set.of("NETWORK"), 6, 5);
        dbIncident = TestFixtures.classifiedIncident(1, Priority.P3, TestFixtures.service(1, "DB", 5), TestFixtures.category(1, "DATABASE"));
    }

    private AssignmentContext ctx(Map<Long, Integer> load, Map<Long, Long> history) {
        return new AssignmentContext(dbIncident, List.of(dbJunior, dbSenior, netGuy), load, history);
    }

    @Test
    @DisplayName("TC-18 Skill-based picks a skilled technician, least loaded first")
    void skillBased() {
        var r = new SkillBasedAssignmentStrategy().select(ctx(Map.of(1L, 0, 2L, 3), Map.of())).orElseThrow();
        assertThat(r.technician()).isEqualTo(dbJunior);
        var ranked = new SkillBasedAssignmentStrategy().rank(ctx(Map.of(), Map.of()));
        assertThat(ranked.get(ranked.size() - 1).technician()).isEqualTo(netGuy); // no skill -> last
    }

    @Test
    @DisplayName("Load-based ignores skills and picks the most spare capacity")
    void loadBased() {
        var r = new LeastLoadedAssignmentStrategy().select(ctx(Map.of(1L, 4, 2L, 2, 3L, 0), Map.of())).orElseThrow();
        assertThat(r.technician()).isEqualTo(netGuy);
    }

    @Test
    @DisplayName("Round-robin rotates among skilled technicians")
    void roundRobin() {
        var rr = new RoundRobinAssignmentStrategy();
        var first = rr.select(ctx(Map.of(), Map.of())).orElseThrow().technician();
        var second = rr.select(ctx(Map.of(), Map.of())).orElseThrow().technician();
        var third = rr.select(ctx(Map.of(), Map.of())).orElseThrow().technician();
        assertThat(first).isEqualTo(dbJunior);
        assertThat(second).isEqualTo(dbSenior);
        assertThat(third).isEqualTo(dbJunior);
    }

    @Test
    @DisplayName("TC-19 Smart recommendation favours experience and service history and explains its score")
    void smart() {
        var ranked = new SmartRecommendationStrategy().rank(ctx(Map.of(), Map.of(2L, 4L)));
        assertThat(ranked.get(0).technician()).isEqualTo(dbSenior);
        assertThat(ranked.get(0).breakdown()).containsKeys("skill", "workload", "experience", "serviceHistory");
        assertThat(ranked.get(0).reason()).contains("Skill match");
    }

    @Test
    @DisplayName("No eligible candidates -> no assignment (all strategies)")
    void emptyCandidates() {
        var empty = new AssignmentContext(dbIncident, List.of(), Map.of(), Map.of());
        List<AssignmentStrategy> all = List.of(new SkillBasedAssignmentStrategy(), new LeastLoadedAssignmentStrategy(),
                new RoundRobinAssignmentStrategy(), new SmartRecommendationStrategy());
        all.forEach(s -> assertThat(s.select(empty)).isEmpty());
    }

    @Test
    @DisplayName("Technician capacity rule: cannot take work at max workload or when unavailable")
    void capacity() {
        assertThat(dbJunior.canTakeMoreWork(4)).isTrue();
        assertThat(dbJunior.canTakeMoreWork(5)).isFalse();
        dbJunior.setAvailable(false);
        assertThat(dbJunior.canTakeMoreWork(0)).isFalse();
    }
}

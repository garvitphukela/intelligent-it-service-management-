package com.itsm.intelligence;

import com.itsm.config.ItsmProperties;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Correlates open incidents through the service dependency graph. If several incidents
 * on different services all depend on the same underlying service, that service is the
 * suspected root cause (e.g. Payment + Email + VPN failing together -> Identity/Auth).
 */
@Component
public class IncidentCorrelator {

    public record Cluster(ITService rootService, List<Incident> incidents, Set<String> affectedServices,
                          long directlyOnRoot, String explanation) {
        public String key() {
            return rootService.getId() + ":" + incidents.stream().map(Incident::getId).sorted().toList();
        }
    }

    private final IncidentRepository incidents;
    private final ItsmProperties props;
    private final Clock clock;

    public IncidentCorrelator(IncidentRepository incidents, ItsmProperties props, Clock clock) {
        this.incidents = incidents;
        this.props = props;
        this.clock = clock;
    }

    public List<Cluster> findClusters() {
        var cfg = props.getIntelligence();
        Instant since = Instant.now(clock).minus(Duration.ofMinutes(cfg.getCorrelationWindowMinutes()));
        List<Incident> recent = incidents.findByStatusIn(IncidentStatus.openStatuses()).stream()
                .filter(i -> i.getCreatedAt().isAfter(since))
                .filter(i -> i.getMasterIncident() == null)
                .filter(i -> i.getPriority() != null && i.getPriority() != Priority.P4) // ignore low-priority noise
                .toList();
        return cluster(recent, cfg.getCorrelationMinIncidents());
    }

    /** Pure function, unit-testable without a database. */
    public static List<Cluster> cluster(List<Incident> recent, int minSize) {
        Map<ITService, List<Incident>> byRoot = new LinkedHashMap<>();
        for (Incident i : recent) {
            Set<ITService> candidates = new LinkedHashSet<>();
            candidates.add(i.getService());
            candidates.addAll(i.getService().allDependencies());
            for (ITService root : candidates) byRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
        }

        List<Cluster> clusters = new ArrayList<>();
        for (var e : byRoot.entrySet()) {
            List<Incident> group = e.getValue();
            if (group.size() < minSize) continue;
            ITService root = e.getKey();
            long direct = group.stream().filter(i -> i.getService().equals(root)).count();
            Set<String> services = new TreeSet<>();
            group.forEach(i -> services.add(i.getService().getName()));
            // A root explains the cluster only if incidents span services or several hit it directly
            if (services.size() == 1 && direct < minSize) continue;
            String why = services.size() == 1
                    ? group.size() + " incidents reported on " + root.getName() + " within the window"
                    : group.size() + " incidents across " + services.size() + " services that all depend on " + root.getName();
            clusters.add(new Cluster(root, group, services, direct, why));
        }
        // Keep one explanation per incident set: prefer roots with direct reports, then the most specific
        // shared dependency (e.g. Identity & Auth rather than the Network underneath it).
        clusters.sort(Comparator.comparingInt((Cluster c) -> c.incidents().size()).reversed()
                .thenComparingLong(c -> -c.directlyOnRoot())
                .thenComparingInt(c -> -c.rootService().allDependencies().size()));
        // Drop clusters whose incidents are already explained by a larger (or equal) accepted cluster.
        List<Cluster> result = new ArrayList<>();
        List<Set<Long>> accepted = new ArrayList<>();
        for (Cluster c : clusters) {
            Set<Long> ids = new HashSet<>();
            c.incidents().forEach(i -> ids.add(i.getId()));
            if (accepted.stream().anyMatch(a -> a.containsAll(ids))) continue;
            accepted.add(ids);
            result.add(c);
        }
        return result;
    }
}

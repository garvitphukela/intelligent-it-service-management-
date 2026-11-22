package com.itsm.service;

import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.*;
import com.itsm.domain.user.Technician;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Operational analytics. Aggregates in memory for clarity; at scale these would move
 * to SQL aggregate queries or a read-model (see report section on scalability).
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    public record Overview(long total, long open, long critical, long resolved, long slaViolations, long unassigned,
                           long awaitingInfo, long majorIncidents, Double avgResponseMinutes, Double avgResolutionMinutes,
                           Double slaCompliancePercent, Double avgRating, long createdToday) {}

    public record TechnicianLoad(Long id, String code, String name, String role, int active, int maxWorkload,
                                 long resolved, Double avgResolutionMinutes, Double avgRating, boolean available, Set<String> skills) {}

    public record ServiceStat(Long id, String name, int criticality, long total, long open, long breached, long p1,
                              Double avgResolutionMinutes) {}

    public record SlaCompliance(String priority, long total, long met, long breached, long running, Double compliancePercent,
                                Double avgResponseMinutes, Double avgResolutionMinutes) {}

    public record TrendPoint(LocalDate date, long created, long resolved, long breached) {}

    public record Count(String key, long count) {}

    private final IncidentRepository incidents;
    private final UserRepository users;
    private final ITServiceRepository services;
    private final AssignmentService assignment;
    private final Clock clock;

    public ReportService(IncidentRepository incidents, UserRepository users, ITServiceRepository services,
                         AssignmentService assignment, Clock clock) {
        this.incidents = incidents;
        this.users = users;
        this.services = services;
        this.assignment = assignment;
        this.clock = clock;
    }

    private static final Predicate<Incident> RESOLVED = i ->
            EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED).contains(i.getStatus());

    public Overview overview() {
        List<Incident> all = incidents.findAll();
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        long open = all.stream().filter(i -> i.getStatus().isOpen()).count();
        long critical = all.stream().filter(i -> i.getStatus().isOpen() && i.getPriority() == Priority.P1).count();
        long resolved = all.stream().filter(RESOLVED).count();
        long breached = all.stream().filter(i -> i.getSla().isBreached()).count();
        long unassigned = all.stream().filter(i -> i.getStatus().isOpen() && i.getAssignedTechnician() == null).count();
        long awaiting = all.stream().filter(i -> i.getStatus() == IncidentStatus.AWAITING_INFO).count();
        long major = all.stream().filter(i -> i.getStatus().isOpen() && i.isMajorIncident()).count();
        long withOutcome = all.stream().filter(i -> RESOLVED.test(i) || i.getSla().isBreached()).count();
        Double compliance = withOutcome == 0 ? null : round(100.0 * (withOutcome - breached) / withOutcome);
        long createdToday = all.stream().filter(i -> i.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today)).count();
        return new Overview(all.size(), open, critical, resolved, breached, unassigned, awaiting, major,
                avgMinutes(all, i -> i.getSla().responseTime()), avgMinutes(all, i -> i.getSla().resolutionTime()),
                compliance, avgRating(all), createdToday);
    }

    public List<TechnicianLoad> technicianWorkload() {
        Map<Long, Integer> load = assignment.currentWorkloads();
        List<Incident> all = incidents.findAll();
        Map<Long, List<Incident>> byTech = all.stream().filter(i -> i.getAssignedTechnician() != null)
                .collect(Collectors.groupingBy(i -> i.getAssignedTechnician().getId()));
        List<TechnicianLoad> rows = new ArrayList<>();
        for (Technician t : users.findAllTechnicians()) {
            List<Incident> mine = byTech.getOrDefault(t.getId(), List.of());
            rows.add(new TechnicianLoad(t.getId(), t.getCode(), t.getFullName(), t.getRole().name(),
                    load.getOrDefault(t.getId(), 0), t.getMaxWorkload(), mine.stream().filter(RESOLVED).count(),
                    avgMinutes(mine, i -> i.getSla().resolutionTime()), avgRating(mine), t.isAvailable() && t.isActive(), t.getSkills()));
        }
        return rows;
    }

    public List<ServiceStat> serviceAnalysis() {
        Map<Long, List<Incident>> bySvc = incidents.findAll().stream().collect(Collectors.groupingBy(i -> i.getService().getId()));
        List<ServiceStat> rows = new ArrayList<>();
        for (ITService s : services.findAll()) {
            List<Incident> list = bySvc.getOrDefault(s.getId(), List.of());
            rows.add(new ServiceStat(s.getId(), s.getName(), s.getBusinessCriticality(), list.size(),
                    list.stream().filter(i -> i.getStatus().isOpen()).count(),
                    list.stream().filter(i -> i.getSla().isBreached()).count(),
                    list.stream().filter(i -> i.getPriority() == Priority.P1).count(),
                    avgMinutes(list, i -> i.getSla().resolutionTime())));
        }
        rows.sort(Comparator.comparingLong(ServiceStat::total).reversed());
        return rows;
    }

    public List<SlaCompliance> slaCompliance() {
        Map<Priority, List<Incident>> byP = incidents.findAll().stream().filter(i -> i.getPriority() != null)
                .collect(Collectors.groupingBy(Incident::getPriority));
        List<SlaCompliance> rows = new ArrayList<>();
        for (Priority p : Priority.values()) {
            List<Incident> list = byP.getOrDefault(p, List.of()).stream().filter(i -> i.getStatus() != IncidentStatus.CANCELLED).toList();
            long breached = list.stream().filter(i -> i.getSla().isBreached()).count();
            long met = list.stream().filter(i -> RESOLVED.test(i) && !i.getSla().isBreached()).count();
            long running = list.size() - breached - met;
            Double pct = met + breached == 0 ? null : round(100.0 * met / (met + breached));
            rows.add(new SlaCompliance(p.name(), list.size(), met, breached, running, pct,
                    avgMinutes(list, i -> i.getSla().responseTime()), avgMinutes(list, i -> i.getSla().resolutionTime())));
        }
        return rows;
    }

    public List<TrendPoint> trend(int days) {
        int span = Math.max(1, Math.min(90, days));
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate start = today.minusDays(span - 1L);
        Map<LocalDate, long[]> buckets = new TreeMap<>();
        for (int d = 0; d < span; d++) buckets.put(start.plusDays(d), new long[3]);
        for (Incident i : incidents.findAll()) {
            LocalDate c = i.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (buckets.containsKey(c)) {
                buckets.get(c)[0]++;
                if (i.getSla().isBreached()) buckets.get(c)[2]++;
            }
            Instant r = i.getSla().getResolvedAt();
            if (r != null) {
                LocalDate rd = r.atZone(ZoneOffset.UTC).toLocalDate();
                if (buckets.containsKey(rd)) buckets.get(rd)[1]++;
            }
        }
        return buckets.entrySet().stream().map(e -> new TrendPoint(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
    }

    public List<Count> byCategory() {
        return count(i -> i.getCategory() == null ? "UNCATEGORISED" : i.getCategory().getName());
    }

    public List<Count> byStatus() { return count(i -> i.getStatus().name()); }

    public List<Count> byPriority() {
        Map<String, Long> m = incidents.findAll().stream().filter(i -> i.getPriority() != null)
                .collect(Collectors.groupingBy(i -> i.getPriority().name(), TreeMap::new, Collectors.counting()));
        return m.entrySet().stream().map(e -> new Count(e.getKey(), e.getValue())).toList();
    }

    private List<Count> count(java.util.function.Function<Incident, String> key) {
        return incidents.findAll().stream().collect(Collectors.groupingBy(key, Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new Count(e.getKey(), e.getValue())).toList();
    }

    private static Double avgMinutes(List<Incident> list, java.util.function.Function<Incident, Duration> f) {
        OptionalDouble avg = list.stream().map(f).filter(Objects::nonNull).mapToLong(Duration::getSeconds).average();
        return avg.isPresent() ? round(avg.getAsDouble() / 60.0) : null;
    }

    private static Double avgRating(List<Incident> list) {
        OptionalDouble avg = list.stream().map(Incident::getFeedback).filter(f -> f != null && f.getRating() != null)
                .mapToInt(Feedback::getRating).average();
        return avg.isPresent() ? round(avg.getAsDouble()) : null;
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}

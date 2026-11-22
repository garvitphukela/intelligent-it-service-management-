package com.itsm.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Outbound API views. Entities are never serialised directly. */
public final class Views {

    private Views() {}

    public record Ref(Long id, String name) {}

    public record UserSummary(Long id, String code, String fullName, String email, String role, String title) {}

    public record UserView(Long id, String code, String fullName, String email, String role, String title,
                           String phone, String department, boolean active, Instant createdAt,
                           Set<String> skills, Integer maxWorkload, Integer experienceYears, Boolean available,
                           Integer workload, Set<String> preferredChannels) {}

    public record AuthResponse(String token, Instant expiresAt, UserView user) {}

    public record ServiceView(Long id, String name, String description, int businessCriticality, int userBase,
                              String ownerTeam, boolean active, List<Ref> dependsOn) {}

    public record CategoryView(Long id, String name, String description, String requiredSkill,
                               Set<String> keywords, boolean active) {}

    public record SlaPolicyView(Long id, String name, Long serviceId, String serviceName, String priority,
                                int responseMinutes, int resolutionMinutes, int warningThresholdPercent) {}

    public record SlaView(String policyName, Integer responseMinutes, Integer resolutionMinutes,
                          Instant startedAt, Instant responseDueAt, Instant resolutionDueAt, Instant respondedAt,
                          Instant resolvedAt, Instant pausedAt, boolean responseBreached, boolean resolutionBreached,
                          String status, double elapsedPercent, double timeScale) {}

    public record NoteView(Long id, String type, String text, String authorName, String authorRole, Instant createdAt) {}

    public record ResolutionView(String summary, String rootCause, String steps, String resolvedBy, Instant resolvedAt) {}

    public record FeedbackView(Integer rating, String comment, Instant submittedAt) {}

    public record IncidentSummary(Long id, String number, String title, String status, String priority,
                                  String category, String service, Long serviceId, String reporterName, String reporterCode,
                                  String technicianName, String technicianCode, Long technicianId,
                                  String slaStatus, Instant responseDueAt, Instant resolutionDueAt,
                                  Instant createdAt, Instant updatedAt, int escalationLevel, boolean majorIncident,
                                  String masterNumber) {}

    public record IncidentDetail(Long id, String number, String title, String description, String status,
                                 String priority, double priorityScore, String priorityRationale,
                                 String impact, String urgency, int affectedUsers,
                                 Ref category, boolean categoryAutoAssigned, double categoryConfidence,
                                 ServiceView service, UserSummary reporter, UserSummary technician,
                                 SlaView sla, int escalationLevel, String escalationLevelName,
                                 ResolutionView resolution, FeedbackView feedback,
                                 String closureNotes, String closedBy, Instant closedAt, String cancellationReason,
                                 Ref masterIncident, List<Ref> duplicates, boolean majorIncident,
                                 List<NoteView> notes, List<String> allowedActions,
                                 Instant createdAt, Instant updatedAt, long version) {}

    public record DuplicateView(Long id, String number, String title, String status, String service,
                                double similarity, boolean likelyDuplicate, List<String> reasons) {}

    public record CreateIncidentResponse(IncidentDetail incident, List<DuplicateView> possibleDuplicates) {}

    public record AuditView(Long id, Instant timestamp, String actorName, String action, Long incidentId,
                            String incidentNumber, String previousState, String newState, String description) {}

    public record TechnicianScoreView(UserSummary technician, double score, int workload, int maxWorkload,
                                      Map<String, Double> breakdown, String reason) {}

    public record CategorySuggestionView(Long id, String name, double confidence, List<String> matchedTerms) {}

    public record ResolutionSuggestionView(Long incidentId, String incidentNumber, String title, double similarity,
                                           String summary, String rootCause, String steps) {}

    public record PriorityPreview(String priority, String label, double score, String rationale, String strategy,
                                  Integer responseMinutes, Integer resolutionMinutes) {}

    public record AnalysisView(List<CategorySuggestionView> categories, List<DuplicateView> duplicates,
                               List<ResolutionSuggestionView> resolutions, PriorityPreview priority) {}

    public record CorrelationView(Ref rootService, List<IncidentSummary> incidents, Set<String> affectedServices,
                                  long directlyOnRoot, String explanation) {}

    public record SlaRiskView(IncidentSummary incident, double riskScore, Instant predictedResolution, String reason) {}

    public record StrategyView(String key, String name, String description) {}

    public record SettingsView(String assignmentStrategy, String priorityStrategy, boolean autoAssign,
                               List<StrategyView> assignmentStrategies, List<StrategyView> priorityStrategies,
                               double slaTimeScale) {}

    public record WorkloadView(UserView technician, int active, int maxWorkload, long resolvedTotal,
                               Double avgResolutionMinutes, Double avgRating, List<IncidentSummary> incidents) {}

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageView<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}

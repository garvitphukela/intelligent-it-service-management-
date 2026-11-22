package com.itsm.web.dto;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.domain.user.Role;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Set;

/** Inbound API payloads. Bean Validation annotations enforce input rules at the boundary. */
public final class Requests {

    private Requests() {}

    public record Login(@NotBlank @Email String email, @NotBlank String password) {}

    public record Register(@NotBlank @Size(max = 120) String fullName,
                           @NotBlank @Email @Size(max = 160) String email,
                           @NotBlank @Size(min = 8, max = 100) String password,
                           @Size(max = 30) String phone,
                           @Size(max = 80) String department) {}

    public record CreateIncident(@NotBlank @Size(min = 5, max = 200) String title,
                                 @NotBlank @Size(min = 10, max = 4000) String description,
                                 @NotNull Long serviceId,
                                 Long categoryId,
                                 @NotNull Impact impact,
                                 @NotNull Urgency urgency,
                                 @Min(1) @Max(1_000_000) Integer affectedUsers,
                                 Boolean force) {}

    public record UpdateIncident(@Size(min = 5, max = 200) String title,
                                 @Size(min = 10, max = 4000) String description,
                                 Long serviceId, Long categoryId, Impact impact, Urgency urgency,
                                 @Min(1) @Max(1_000_000) Integer affectedUsers) {}

    public record Assign(Long technicianId) {}

    public record Message(@NotBlank @Size(max = 2000) String message) {}

    public record OptionalMessage(@Size(max = 2000) String message) {}

    public record Resolve(@NotBlank @Size(max = 1000) String summary,
                          @Size(max = 1000) String rootCause,
                          @Size(max = 4000) String steps) {}

    public record Note(@NotBlank @Size(max = 4000) String text, Boolean internal) {}

    public record FeedbackReq(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {}

    public record LinkDuplicate(@NotNull Long masterId) {}

    public record Analyze(@Size(max = 200) String title, @Size(max = 4000) String description,
                          Long serviceId, Long categoryId, Impact impact, Urgency urgency, Integer affectedUsers) {}

    public record CreateUser(@NotNull Role role, @NotBlank @Size(max = 120) String fullName,
                             @NotBlank @Email String email, @NotBlank @Size(min = 8) String password,
                             String phone, String department, Set<String> skills,
                             @Min(0) @Max(50) Integer experienceYears, @Min(1) @Max(50) Integer maxWorkload) {}

    public record UpdateUser(@Size(max = 120) String fullName, String phone, String department, Boolean active,
                             Set<String> skills, @Min(0) @Max(50) Integer experienceYears,
                             @Min(1) @Max(50) Integer maxWorkload, Boolean available,
                             Set<NotificationChannelType> preferredChannels, @Size(min = 8) String newPassword) {}

    public record UpdateProfile(@Size(max = 120) String fullName, String phone, String department,
                                Set<NotificationChannelType> preferredChannels) {}

    public record ServiceReq(@NotBlank @Size(max = 80) String name, @Size(max = 500) String description,
                             @NotNull @Min(1) @Max(5) Integer businessCriticality, @Min(0) Integer userBase,
                             String ownerTeam, Boolean active, List<Long> dependsOnIds) {}

    public record CategoryReq(@NotBlank @Size(max = 60) String name, @Size(max = 300) String description,
                              @Size(max = 40) String requiredSkill, List<String> keywords, Boolean active) {}

    public record SlaPolicyReq(@NotBlank String name, Long serviceId, @NotNull Priority priority,
                               @NotNull @Min(1) Integer responseMinutes, @NotNull @Min(1) Integer resolutionMinutes,
                               @Min(10) @Max(99) Integer warningThresholdPercent) {}

    public record Settings(String assignmentStrategy, String priorityStrategy, Boolean autoAssign) {}

    public record Availability(@jakarta.validation.constraints.NotNull Boolean available) {}
}

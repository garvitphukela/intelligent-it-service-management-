package com.itsm.domain.user;

import java.util.Set;

/**
 * Factory pattern: centralises creation of the correct {@link User} subclass
 * for a given {@link Role}, so callers never use {@code new Technician(...)} etc. directly.
 */
public final class UserFactory {

    private UserFactory() {}

    public record UserSpec(Role role, String code, String fullName, String email, String passwordHash,
                           Set<String> skills, Integer experienceYears, Integer maxWorkload) {
        public static UserSpec basic(Role role, String code, String fullName, String email, String hash) {
            return new UserSpec(role, code, fullName, email, hash, Set.of(), 0, null);
        }
    }

    public static User create(UserSpec s) {
        int exp = s.experienceYears() == null ? 0 : s.experienceYears();
        int max = s.maxWorkload() == null ? Technician.DEFAULT_MAX_WORKLOAD : s.maxWorkload();
        return switch (s.role()) {
            case EMPLOYEE -> new Employee(s.code(), s.fullName(), s.email(), s.passwordHash());
            case TECHNICIAN -> new Technician(s.code(), s.fullName(), s.email(), s.passwordHash(), s.skills(), exp, max);
            case TEAM_LEAD -> new TeamLead(s.code(), s.fullName(), s.email(), s.passwordHash(), s.skills(), exp, max);
            case ADMIN -> new Administrator(s.code(), s.fullName(), s.email(), s.passwordHash());
            case MANAGER -> new ITManager(s.code(), s.fullName(), s.email(), s.passwordHash());
        };
    }

    /** Prefix used when auto-generating user codes. */
    public static String codePrefix(Role role) {
        return switch (role) {
            case EMPLOYEE -> "E";
            case TECHNICIAN -> "T";
            case TEAM_LEAD -> "L";
            case ADMIN -> "A";
            case MANAGER -> "M";
        };
    }
}

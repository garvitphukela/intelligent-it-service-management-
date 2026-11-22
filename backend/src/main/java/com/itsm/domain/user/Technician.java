package com.itsm.domain.user;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** A support engineer who investigates and resolves incidents. */
@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    public static final int DEFAULT_MAX_WORKLOAD = 5;

    /** Skill tags, matched against {@code Category.requiredSkill}. Stored upper-case. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill", length = 40)
    private Set<String> skills = new HashSet<>();

    private int maxWorkload = DEFAULT_MAX_WORKLOAD;

    private int experienceYears;

    private boolean available = true;

    protected Technician() {}

    public Technician(String code, String fullName, String email, String passwordHash,
                      Set<String> skills, int experienceYears, int maxWorkload) {
        super(code, fullName, email, passwordHash);
        setSkills(skills);
        this.experienceYears = Math.max(0, experienceYears);
        this.maxWorkload = maxWorkload > 0 ? maxWorkload : DEFAULT_MAX_WORKLOAD;
    }

    @Override
    public Role getRole() { return Role.TECHNICIAN; }

    @Override
    public String getDisplayTitle() { return "Support Engineer"; }

    /** Escalation tier this technician belongs to (0 = first line). */
    public int getEscalationTier() { return 0; }

    public boolean hasSkill(String skill) {
        return skill != null && skills.contains(skill.toUpperCase(Locale.ROOT));
    }

    public boolean canTakeMoreWork(int currentWorkload) {
        return isActive() && available && currentWorkload < maxWorkload;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills == null ? new HashSet<>() :
                skills.stream().filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toCollection(HashSet::new));
    }

    public void updateCapacity(int maxWorkload, int experienceYears) {
        if (maxWorkload <= 0) throw new IllegalArgumentException("maxWorkload must be positive");
        this.maxWorkload = maxWorkload;
        this.experienceYears = Math.max(0, experienceYears);
    }

    public void setAvailable(boolean available) { this.available = available; }

    public Set<String> getSkills() { return Set.copyOf(skills); }
    public int getMaxWorkload() { return maxWorkload; }
    public int getExperienceYears() { return experienceYears; }
    public boolean isAvailable() { return available; }
}

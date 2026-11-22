package com.itsm.domain.catalog;

import jakarta.persistence.*;

import java.util.*;

/**
 * An IT service offered by the organisation (Payment, Database, Network...).
 * Services form a dependency graph ({@link #dependsOn}) used for incident correlation
 * and root-cause hints. Aggregation: a service references other services but does not own them.
 */
@Entity
@Table(name = "it_services")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    /** 1 (low) .. 5 (mission critical). */
    private int businessCriticality = 3;

    /** Rough number of users who depend on this service. */
    private int userBase = 100;

    @Column(length = 120)
    private String ownerTeam;

    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "service_dependencies",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_id"))
    private Set<ITService> dependsOn = new HashSet<>();

    protected ITService() {}

    public ITService(String name, String description, int businessCriticality, int userBase, String ownerTeam) {
        this.name = name;
        this.description = description;
        setBusinessCriticality(businessCriticality);
        this.userBase = Math.max(0, userBase);
        this.ownerTeam = ownerTeam;
    }

    public void update(String name, String description, int businessCriticality, int userBase, String ownerTeam, boolean active) {
        this.name = name;
        this.description = description;
        setBusinessCriticality(businessCriticality);
        this.userBase = Math.max(0, userBase);
        this.ownerTeam = ownerTeam;
        this.active = active;
    }

    private void setBusinessCriticality(int c) {
        if (c < 1 || c > 5) throw new IllegalArgumentException("Business criticality must be between 1 and 5");
        this.businessCriticality = c;
    }

    public void addDependency(ITService other) {
        if (other == null || other == this) return;
        if (other.transitivelyDependsOn(this)) {
            throw new IllegalArgumentException("Circular dependency: " + other.name + " already depends on " + name);
        }
        dependsOn.add(other);
    }

    public void replaceDependencies(Collection<ITService> deps) {
        dependsOn.clear();
        deps.forEach(this::addDependency);
    }

    /** Breadth-first walk of the dependency graph. */
    public boolean transitivelyDependsOn(ITService target) {
        return allDependencies().contains(target);
    }

    public Set<ITService> allDependencies() {
        Set<ITService> seen = new LinkedHashSet<>();
        Deque<ITService> queue = new ArrayDeque<>(dependsOn);
        while (!queue.isEmpty()) {
            ITService s = queue.poll();
            if (seen.add(s)) queue.addAll(s.dependsOn);
        }
        return seen;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getBusinessCriticality() { return businessCriticality; }
    public int getUserBase() { return userBase; }
    public String getOwnerTeam() { return ownerTeam; }
    public boolean isActive() { return active; }
    public Set<ITService> getDependsOn() { return Collections.unmodifiableSet(dependsOn); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ITService other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return ITService.class.hashCode(); }
}

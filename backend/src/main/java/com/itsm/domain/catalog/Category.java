package com.itsm.domain.catalog;

import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Incident category. Modelled as data (not an enum) so administrators can add new
 * categories at runtime without touching unrelated code (Open/Closed principle).
 * The keyword list drives the automatic categoriser; {@code requiredSkill} drives
 * skill-based technician assignment.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(nullable = false, length = 40)
    private String requiredSkill;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "keyword", length = 60)
    private Set<String> keywords = new HashSet<>();

    private boolean active = true;

    protected Category() {}

    public Category(String name, String description, String requiredSkill, Collection<String> keywords) {
        update(name, description, requiredSkill, keywords, true);
    }

    public void update(String name, String description, String requiredSkill, Collection<String> keywords, boolean active) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Category name is required");
        this.name = name.trim().toUpperCase(Locale.ROOT);
        this.description = description;
        this.requiredSkill = (requiredSkill == null || requiredSkill.isBlank() ? this.name : requiredSkill.trim())
                .toUpperCase(Locale.ROOT);
        this.keywords = keywords == null ? new HashSet<>() :
                keywords.stream().filter(k -> k != null && !k.isBlank())
                        .map(k -> k.trim().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toCollection(HashSet::new));
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRequiredSkill() { return requiredSkill; }
    public Set<String> getKeywords() { return Set.copyOf(keywords); }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Category.class.hashCode(); }
}

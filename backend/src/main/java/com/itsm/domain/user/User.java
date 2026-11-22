package com.itsm.domain.user;

import com.itsm.domain.notification.NotificationChannelType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Abstract base for every person who uses the system.
 * <p>
 * Uses single-table inheritance: each concrete subclass is a genuine "is-a"
 * specialisation (an Employee <i>is a</i> User, a TeamLead <i>is a</i> Technician).
 * State is private and only mutated through intention-revealing methods (encapsulation).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human friendly code, e.g. E102 or T205. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @Column(length = 80)
    private String department;

    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_channels", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<NotificationChannelType> preferredChannels = EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL);

    protected User() {
        // for JPA
    }

    protected User(String code, String fullName, String email, String passwordHash) {
        this.code = code;
        this.fullName = fullName;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
    }

    /** Polymorphic: every subclass reports its own role. */
    public abstract Role getRole();

    /** Polymorphic: what the user may see on the dashboard. */
    public abstract String getDisplayTitle();

    public boolean canManageIncidents() {
        return getRole().isManagement();
    }

    // ---- behaviour ----

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void updateProfile(String fullName, String phone, String department) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName;
        this.phone = phone;
        this.department = department;
    }

    public void setPreferredChannels(Set<NotificationChannelType> channels) {
        this.preferredChannels = channels == null || channels.isEmpty()
                ? EnumSet.of(NotificationChannelType.IN_APP)
                : EnumSet.copyOf(channels);
    }

    // ---- accessors ----

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<NotificationChannelType> getPreferredChannels() { return Set.copyOf(preferredChannels); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getRole() + " " + code + " (" + fullName + ")";
    }
}

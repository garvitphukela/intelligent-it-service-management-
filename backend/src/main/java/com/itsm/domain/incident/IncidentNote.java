package com.itsm.domain.incident;

import jakarta.persistence.*;

import java.time.Instant;

/** Part of the Incident aggregate (composition): cannot exist without its incident. */
@Entity
@Table(name = "incident_notes")
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    private Long authorId;

    @Column(nullable = false, length = 140)
    private String authorName;

    @Column(length = 20)
    private String authorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteType type;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    protected IncidentNote() {}

    IncidentNote(Incident incident, Long authorId, String authorName, String authorRole, NoteType type, String text, Instant createdAt) {
        this.incident = incident;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.type = type;
        this.text = text;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public NoteType getType() { return type; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.itsm.domain.incident;

/** How quickly the business needs a resolution. */
public enum Urgency {
    LOW(1, "Can wait, no deadline"),
    MEDIUM(2, "Work is slowed down"),
    HIGH(3, "Work is blocked"),
    CRITICAL(4, "Immediate financial / safety / reputational damage");

    private final int weight;
    private final String description;

    Urgency(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int weight() { return weight; }
    public String description() { return description; }
}

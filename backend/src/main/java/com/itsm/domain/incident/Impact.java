package com.itsm.domain.incident;

/** How widely the business is affected. */
public enum Impact {
    LOW(1, "Single user, workaround available"),
    MEDIUM(2, "Small group or a department"),
    HIGH(3, "Many users or an entire site"),
    EXTENSIVE(4, "Organisation-wide / customer facing outage");

    private final int weight;
    private final String description;

    Impact(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int weight() { return weight; }
    public String description() { return description; }
}

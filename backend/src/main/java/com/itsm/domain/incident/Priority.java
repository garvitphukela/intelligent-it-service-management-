package com.itsm.domain.incident;

public enum Priority {
    P1("Critical"), P2("High"), P3("Medium"), P4("Low");

    private final String label;

    Priority(String label) { this.label = label; }

    public String label() { return label; }

    /** 1 for P1 ... 4 for P4. */
    public int level() { return ordinal() + 1; }

    public boolean isHigherThan(Priority other) { return this.ordinal() < other.ordinal(); }

    /** One step more urgent (P1 stays P1). */
    public Priority raise() { return this == P1 ? P1 : values()[ordinal() - 1]; }

    /** One step less urgent (P4 stays P4). */
    public Priority lower() { return this == P4 ? P4 : values()[ordinal() + 1]; }
}

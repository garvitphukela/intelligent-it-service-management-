package com.itsm.domain.user;

/** System roles. Each role maps to one concrete {@link User} subclass. */
public enum Role {
    EMPLOYEE, TECHNICIAN, TEAM_LEAD, ADMIN, MANAGER;

    public boolean isStaff() {
        return this != EMPLOYEE;
    }

    public boolean isSupportEngineer() {
        return this == TECHNICIAN || this == TEAM_LEAD;
    }

    public boolean isManagement() {
        return this == ADMIN || this == MANAGER;
    }
}

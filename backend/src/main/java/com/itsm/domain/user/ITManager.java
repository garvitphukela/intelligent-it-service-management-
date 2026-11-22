package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** IT manager: final escalation tier, focuses on reporting and SLA oversight. */
@Entity
@DiscriminatorValue("MANAGER")
public class ITManager extends Administrator {

    protected ITManager() {}

    public ITManager(String code, String fullName, String email, String passwordHash) {
        super(code, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.MANAGER; }

    @Override
    public String getDisplayTitle() { return "IT Manager"; }
}

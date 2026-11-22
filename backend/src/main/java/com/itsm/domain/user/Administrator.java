package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** System administrator: configures the platform and monitors operations. */
@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    protected Administrator() {}

    public Administrator(String code, String fullName, String email, String passwordHash) {
        super(code, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.ADMIN; }

    @Override
    public String getDisplayTitle() { return "System Administrator"; }
}

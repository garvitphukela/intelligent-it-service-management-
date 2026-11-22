package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** An employee / customer who reports incidents. */
@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    protected Employee() {}

    public Employee(String code, String fullName, String email, String passwordHash) {
        super(code, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() { return Role.EMPLOYEE; }

    @Override
    public String getDisplayTitle() { return "Employee"; }
}

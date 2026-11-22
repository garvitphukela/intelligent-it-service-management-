package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Set;

/**
 * A team lead <i>is a</i> technician (can still resolve incidents) with extra
 * responsibility: receives escalations of unacknowledged critical incidents.
 */
@Entity
@DiscriminatorValue("TEAM_LEAD")
public class TeamLead extends Technician {

    protected TeamLead() {}

    public TeamLead(String code, String fullName, String email, String passwordHash,
                    Set<String> skills, int experienceYears, int maxWorkload) {
        super(code, fullName, email, passwordHash, skills, experienceYears, maxWorkload);
    }

    @Override
    public Role getRole() { return Role.TEAM_LEAD; }

    @Override
    public String getDisplayTitle() { return "Team Lead"; }

    @Override
    public int getEscalationTier() { return 1; }
}

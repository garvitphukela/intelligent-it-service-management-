package com.itsm.domain;

import com.itsm.TestFixtures;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DomainModelTest {

    @Test
    @DisplayName("TC-23 Factory creates the correct User subclass for each role (inheritance + polymorphism)")
    void userFactory() {
        for (Role r : Role.values()) {
            User u = UserFactory.create(new UserFactory.UserSpec(r, "X1", "Name", "x@y.z", "h", Set.of("NETWORK"), 3, 4));
            assertThat(u.getRole()).isEqualTo(r);
        }
        User lead = UserFactory.create(new UserFactory.UserSpec(Role.TEAM_LEAD, "L1", "Lead", "l@y.z", "h", Set.of("network"), 8, 4));
        assertThat(lead).isInstanceOf(Technician.class).isInstanceOf(TeamLead.class);
        assertThat(((Technician) lead).hasSkill("NETWORK")).isTrue();
        assertThat(((Technician) lead).getEscalationTier()).isEqualTo(1);
        assertThat(UserFactory.create(UserFactory.UserSpec.basic(Role.MANAGER, "M1", "M", "m@y.z", "h"))).isInstanceOf(Administrator.class);
    }

    @Test
    @DisplayName("TC-24 Circular service dependencies are rejected")
    void circularDependency() {
        ITService a = TestFixtures.service(1, "A", 3);
        ITService b = TestFixtures.service(2, "B", 3);
        ITService c = TestFixtures.service(3, "C", 3);
        a.addDependency(b);
        b.addDependency(c);
        assertThat(a.transitivelyDependsOn(c)).isTrue();
        assertThatThrownBy(() -> c.addDependency(a)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Circular");
    }

    @Test
    @DisplayName("Categories normalise their name, skill and keywords")
    void categoryNormalisation() {
        Category c = new Category(" vpn ", null, null, List.of(" Tunnel ", "", "Remote"));
        assertThat(c.getName()).isEqualTo("VPN");
        assertThat(c.getRequiredSkill()).isEqualTo("VPN");
        assertThat(c.getKeywords()).containsExactlyInAnyOrder("tunnel", "remote");
    }

    @Test
    @DisplayName("TC-25 SLA policy validation: response must not exceed resolution, targets positive")
    void slaPolicyValidation() {
        assertThatThrownBy(() -> new SlaPolicy("x", null, Priority.P1, 60, 30, 75)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlaPolicy("x", null, Priority.P1, 0, 30, 75)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlaPolicy("x", null, Priority.P1, 5, 30, 100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Service business criticality must be 1..5")
    void criticalityRange() {
        assertThatThrownBy(() -> new ITService("x", null, 0, 10, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ITService("x", null, 6, 10, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

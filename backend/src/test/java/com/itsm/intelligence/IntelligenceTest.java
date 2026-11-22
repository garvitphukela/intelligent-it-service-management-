package com.itsm.intelligence;

import com.itsm.TestFixtures;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.service.EscalationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.itsm.TestFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceTest {

    @Test
    @DisplayName("Text analyzer maps synonyms to a canonical term and drops stop words")
    void synonyms() {
        assertThat(TextAnalyzer.tokens("Payment application unavailable")).containsExactly("pay", "application", "down");
        assertThat(TextAnalyzer.tokens("The payments are failing")).containsExactly("pay", "fail");
    }

    @Test
    @DisplayName("TC-20 Duplicate detection: the spec's three payment reports are more similar to each other than to an unrelated one")
    void paymentReportsAreSimilar() {
        var corpus = List.of("Payment application unavailable", "Payment transaction failing", "Unable to make payment", "Laptop screen flickering");
        TfIdfModel m = new TfIdfModel(corpus);
        double related = m.similarity(corpus.get(0), corpus.get(1));
        double related2 = m.similarity(corpus.get(1), corpus.get(2));
        double unrelated = m.similarity(corpus.get(0), corpus.get(3));
        assertThat(related).isGreaterThan(0.2);
        assertThat(related2).isGreaterThan(0.2);
        assertThat(unrelated).isZero();
        assertThat(m.similarity("same text here", "same text here")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("TC-21 Correlation: incidents on Email, VPN and Payment point to the shared Identity service")
    void correlationFindsSharedDependency() {
        ITService net = TestFixtures.service(1, "Network", 5);
        ITService auth = TestFixtures.service(2, "Identity", 5);
        ITService mail = TestFixtures.service(3, "Email", 4);
        ITService vpn = TestFixtures.service(4, "VPN", 4);
        ITService pay = TestFixtures.service(5, "Payment", 5);
        auth.addDependency(net);
        mail.addDependency(auth);
        vpn.addDependency(auth);
        pay.addDependency(auth);
        var cat = TestFixtures.category(1, "AUTHENTICATION");
        List<Incident> open = List.of(TestFixtures.classifiedIncident(1, Priority.P1, mail, cat),
                TestFixtures.classifiedIncident(2, Priority.P1, vpn, cat), TestFixtures.classifiedIncident(3, Priority.P1, pay, cat));

        var clusters = IncidentCorrelator.cluster(open, 3);
        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).rootService()).isEqualTo(auth);
        assertThat(clusters.get(0).affectedServices()).containsExactlyInAnyOrder("Email", "VPN", "Payment");
    }

    @Test
    @DisplayName("Correlation needs at least the minimum number of incidents")
    void correlationThreshold() {
        ITService a = TestFixtures.service(1, "A", 3);
        var cat = TestFixtures.category(1, "APPLICATION");
        assertThat(IncidentCorrelator.cluster(List.of(TestFixtures.classifiedIncident(1, Priority.P2, a, cat),
                TestFixtures.classifiedIncident(2, Priority.P2, a, cat)), 3)).isEmpty();
    }

    @Test
    @DisplayName("TC-22 Escalation levels: Team Lead at 100%, Admin at 150%, Manager at 200% of the response window")
    void escalationLevels() {
        Incident i = TestFixtures.classifiedIncident(1, Priority.P1, TestFixtures.service(1, "Pay", 5), TestFixtures.category(1, "APPLICATION"));
        // response window = 5 minutes
        assertThat(EscalationService.targetLevel(i, T0.plusSeconds(299))).isZero();
        assertThat(EscalationService.targetLevel(i, T0.plusSeconds(300))).isEqualTo(1);
        assertThat(EscalationService.targetLevel(i, T0.plusSeconds(450))).isEqualTo(2);
        assertThat(EscalationService.targetLevel(i, T0.plusSeconds(600))).isEqualTo(3);
    }

    @Test
    @DisplayName("Low-priority or acknowledged incidents never escalate")
    void noEscalation() {
        var svc = TestFixtures.service(1, "Pay", 5);
        var cat = TestFixtures.category(1, "APPLICATION");
        Incident p3 = TestFixtures.classifiedIncident(1, Priority.P3, svc, cat);
        assertThat(EscalationService.targetLevel(p3, T0.plusSeconds(10_000))).isZero();
        Incident acked = TestFixtures.classifiedIncident(2, Priority.P1, svc, cat);
        acked.assignTo(TestFixtures.technician(5, java.util.Set.of("APPLICATION"), 3, 5), T0);
        acked.acknowledge(T0.plusSeconds(10));
        assertThat(EscalationService.targetLevel(acked, T0.plusSeconds(10_000))).isZero();
    }
}

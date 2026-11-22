package com.itsm.api;

import com.jayway.jsonpath.JsonPath;
import com.itsm.repository.AuditEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests through the real HTTP layer, security filter chain, services and an
 * in-memory H2 database populated by the seeder.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AuditEntryRepository audit;

    private String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions as(String token, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON));
    }

    private int createIncident(String token, String title, int serviceId, String impact, String urgency) throws Exception {
        String body = as(token, post("/api/incidents").content("""
                {"title":"%s","description":"%s - started this morning and affects many colleagues","serviceId":%d,"impact":"%s","urgency":"%s","affectedUsers":200}
                """.formatted(title, title, serviceId, impact, urgency)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.incident.id");
    }

    @Test
    @DisplayName("TC-26 Valid login returns a JWT and the user profile")
    void loginOk() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@itsm.local\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("TC-27 Invalid login -> 401 INVALID_LOGIN")
    void loginInvalid() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@itsm.local\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN"));
    }

    @Test
    @DisplayName("TC-28 Unauthenticated request -> 401; employee calling admin API -> 403")
    void authorization() throws Exception {
        mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
        as(token("employee@itsm.local"), get("/api/admin/users")).andExpect(status().isForbidden());
        as(token("employee@itsm.local"), get("/api/reports/overview")).andExpect(status().isForbidden());
        as(token("admin@itsm.local"), get("/api/admin/users")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-29 Employees only see their own incidents; viewing another's -> 403")
    void ownership() throws Exception {
        String karan = token("karan@itsm.local");
        int id = createIncident(karan, "Shared drive quota exceeded for finance", 5, "MEDIUM", "MEDIUM");
        String ananya = token("employee@itsm.local");
        as(ananya, get("/api/incidents/" + id)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("UNAUTHORIZED_OPERATION"));
        as(karan, get("/api/incidents/" + id)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-30 Invalid input -> 400 with field errors")
    void validation() throws Exception {
        as(token("employee@itsm.local"), post("/api/incidents").content("{\"title\":\"x\",\"description\":\"\",\"impact\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.serviceId").exists());
        as(token("employee@itsm.local"), post("/api/incidents").content("{\"title\":\"hello world\",\"description\":\"long enough text\",\"serviceId\":1,\"impact\":\"HUGE\",\"urgency\":\"LOW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-31 Unknown service -> 404 SERVICE_NOT_FOUND")
    void serviceNotFound() throws Exception {
        as(token("employee@itsm.local"), post("/api/incidents").content("""
                {"title":"Something broken","description":"Something is broken badly","serviceId":9999,"impact":"LOW","urgency":"LOW"}"""))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    @DisplayName("TC-32 Workflow 1+2: report -> auto classify/prioritise/assign -> work -> resolve -> verify -> close, fully audited")
    void fullWorkflow() throws Exception {
        String emp = token("pooja@itsm.local");
        String body = as(emp, post("/api/incidents").content("""
                {"title":"Payment page returns 503","description":"Customers cannot pay at checkout, all transactions failing","serviceId":6,"impact":"EXTENSIVE","urgency":"CRITICAL","affectedUsers":2000}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incident.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.incident.priority").value("P1"))
                .andExpect(jsonPath("$.incident.category.name").value("APPLICATION"))
                .andExpect(jsonPath("$.incident.technician.code").exists())
                .andExpect(jsonPath("$.incident.sla.policyName").value("Payments P1 - Revenue critical"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.incident.id");
        String techEmail = JsonPath.read(body, "$.incident.technician.email");
        String tech = token(techEmail);

        as(tech, post("/api/incidents/" + id + "/acknowledge")).andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        as(tech, post("/api/incidents/" + id + "/start")).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        as(tech, post("/api/incidents/" + id + "/notes").content("{\"text\":\"checking pods\",\"internal\":true}")).andExpect(status().isOk());
        as(emp, get("/api/incidents/" + id)).andExpect(jsonPath("$.notes[?(@.type=='WORK_NOTE')]").isEmpty());
        as(tech, post("/api/incidents/" + id + "/resolve").content("{\"summary\":\"Restarted pods\",\"rootCause\":\"leak\"}"))
                .andExpect(jsonPath("$.status").value("RESOLVED")).andExpect(jsonPath("$.sla.status").value("MET"));
        as(emp, post("/api/incidents/" + id + "/verify")).andExpect(jsonPath("$.status").value("VERIFIED"));
        as(emp, post("/api/incidents/" + id + "/feedback").content("{\"rating\":5,\"comment\":\"fast\"}")).andExpect(status().isOk());
        as(tech, post("/api/incidents/" + id + "/close").content("{\"message\":\"done\"}")).andExpect(jsonPath("$.status").value("CLOSED"));

        var actions = audit.findByIncidentIdOrderByTimestampAscIdAsc((long) id).stream().map(a -> a.getAction()).toList();
        assertThat(actions).containsSubsequence("INCIDENT_CREATED", "INCIDENT_CLASSIFIED", "PRIORITY_CALCULATED", "INCIDENT_ASSIGNED",
                "INCIDENT_ACKNOWLEDGED", "INCIDENT_WORK_STARTED", "INCIDENT_RESOLVED", "INCIDENT_VERIFIED", "INCIDENT_CLOSED");
        as(emp, get("/api/notifications")).andExpect(jsonPath("$.totalElements", greaterThan(0)));
    }

    @Test
    @DisplayName("TC-33 Business rule: an employee cannot resolve; an illegal transition -> 409")
    void businessRules() throws Exception {
        String emp = token("siddharth@itsm.local");
        int id = createIncident(emp, "HR portal leave page error", 9, "MEDIUM", "MEDIUM");
        as(emp, post("/api/incidents/" + id + "/resolve").content("{\"summary\":\"x\"}"))
                .andExpect(status().isForbidden());
        as(token("admin@itsm.local"), post("/api/incidents/" + id + "/close").content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("TC-34 Same reporter submitting the same problem twice -> 409 DUPLICATE_INCIDENT (force overrides)")
    void duplicateIncident() throws Exception {
        String emp = token("karan@itsm.local");
        String req = """
                {"title":"VPN keeps dropping every few minutes","description":"VPN keeps dropping every few minutes when working from home","serviceId":8,"impact":"LOW","urgency":"MEDIUM"%s}""";
        as(emp, post("/api/incidents").content(req.formatted(""))).andExpect(status().isCreated());
        as(emp, post("/api/incidents").content(req.formatted(""))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_INCIDENT"));
        as(emp, post("/api/incidents").content(req.formatted(",\"force\":true"))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-35 Manual assignment to a technician at max workload -> 409 WORKLOAD_EXCEEDED")
    void workloadExceeded() throws Exception {
        String admin = token("admin@itsm.local");
        // make T106 (id 10) capacity 1 and give them one incident
        as(admin, put("/api/admin/users/10").content("{\"maxWorkload\":1}")).andExpect(status().isOk());
        String emp = token("employee@itsm.local");
        int a = createIncident(emp, "Cloud cluster pods pending forever", 4, "LOW", "LOW");
        int b = createIncident(emp, "Object storage bucket upload error", 5, "LOW", "LOW");
        as(admin, post("/api/incidents/" + a + "/assign").content("{\"technicianId\":10}"));
        as(admin, post("/api/incidents/" + b + "/assign").content("{\"technicianId\":10}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WORKLOAD_EXCEEDED"));
        as(admin, put("/api/admin/users/10").content("{\"maxWorkload\":5}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-36 Search filters combine (priority + status + service) and employee scope is enforced")
    void search() throws Exception {
        String admin = token("admin@itsm.local");
        as(admin, get("/api/incidents?priority=P1&serviceId=6&status=CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].priority", everyItem(is("P1"))))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("CLOSED"))));
        as(admin, get("/api/incidents?number=INC1001")).andExpect(jsonPath("$.totalElements").value(1));
        as(admin, get("/api/incidents?sort=hacker")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-37 Admin can switch the assignment strategy at runtime; unknown strategy -> 400")
    void strategySwitch() throws Exception {
        String admin = token("admin@itsm.local");
        as(admin, put("/api/admin/settings").content("{\"assignmentStrategy\":\"LOAD_BASED\"}"))
                .andExpect(jsonPath("$.assignmentStrategy").value("LOAD_BASED"));
        as(admin, put("/api/admin/settings").content("{\"assignmentStrategy\":\"MAGIC\"}")).andExpect(status().isBadRequest());
        as(admin, put("/api/admin/settings").content("{\"assignmentStrategy\":\"SMART\"}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-38 Default SLA policies cannot be deleted; reports respond")
    void slaAndReports() throws Exception {
        String admin = token("admin@itsm.local");
        as(admin, delete("/api/admin/sla-policies/1")).andExpect(status().isConflict());
        as(admin, get("/api/reports/overview")).andExpect(status().isOk()).andExpect(jsonPath("$.total", greaterThan(40)));
        as(admin, get("/api/reports/sla-compliance")).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("TC-39 Live analysis suggests a category, a priority and known fixes")
    void analysis() throws Exception {
        as(token("employee@itsm.local"), post("/api/intelligence/analyze").content("""
                {"title":"SSO login loop","description":"Cannot sign in, the single sign-on page keeps looping","serviceId":2,"impact":"HIGH","urgency":"HIGH","affectedUsers":300}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.priority.priority").value("P1"))
                .andExpect(jsonPath("$.resolutions", not(empty())));
    }
}

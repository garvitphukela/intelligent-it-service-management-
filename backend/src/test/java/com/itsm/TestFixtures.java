package com.itsm;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.*;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.TeamLead;
import com.itsm.domain.user.Technician;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Builders for domain objects in pure unit tests (no Spring, no database). */
public final class TestFixtures {

    public static final Instant T0 = Instant.parse("2026-11-18T10:00:00Z");

    private TestFixtures() {}

    public static <T> T withId(T entity, long id) {
        try {
            Class<?> c = entity.getClass();
            while (c != null) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getName().equals("id")) {
                        f.setAccessible(true);
                        f.set(entity, id);
                        return entity;
                    }
                }
                c = c.getSuperclass();
            }
            throw new IllegalStateException("no id field");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static ITService service(long id, String name, int criticality) {
        return withId(new ITService(name, name + " service", criticality, 1000, "Team"), id);
    }

    public static Category category(long id, String name) {
        return withId(new Category(name, name, name, List.of(name.toLowerCase())), id);
    }

    public static Employee employee(long id) {
        return withId(new Employee("E" + id, "Employee " + id, "e" + id + "@test.local", "hash"), id);
    }

    public static Technician technician(long id, Set<String> skills, int experience, int max) {
        return withId(new Technician("T" + id, "Tech " + id, "t" + id + "@test.local", "hash", skills, experience, max), id);
    }

    public static TeamLead lead(long id, Set<String> skills) {
        return withId(new TeamLead("L" + id, "Lead " + id, "l" + id + "@test.local", "hash", skills, 10, 4), id);
    }

    public static SlaPolicy policy(Priority p, int response, int resolution) {
        return withId(new SlaPolicy("Test " + p, null, p, response, resolution, 75), p.ordinal() + 1L);
    }

    /** A CLASSIFIED incident with category, priority and SLA started at {@link #T0}. */
    public static Incident classifiedIncident(long id, Priority priority, ITService service, Category category) {
        Incident i = withId(new Incident("Payment page down", "Customers cannot pay at checkout", employee(100), service,
                Impact.HIGH, Urgency.HIGH, 10, T0), id);
        i.applyCategory(category, true, 0.9);
        i.applyPriority(priority, 50, "test");
        SlaPolicy p = policy(priority, 5, 30);
        i.applySla(p, Duration.ofMinutes(5), Duration.ofMinutes(30));
        i.markClassified(T0);
        return i;
    }
}

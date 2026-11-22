package com.itsm.config;

import com.itsm.domain.audit.AuditEntry;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.*;
import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.domain.user.*;
import com.itsm.repository.*;
import com.itsm.service.PriorityService;
import com.itsm.service.SlaService;
import com.itsm.service.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Seeds a realistic organisation on first start-up: services with dependencies, categories,
 * SLA policies, users of every role and ~50 historical incidents driven through the real
 * state machine (so analytics, duplicate detection and resolution suggestions have data).
 * All seeded accounts use the password {@value #PASSWORD}.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String PASSWORD = "Password@123";

    private final ItsmProperties props;
    private final UserRepository users;
    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final SlaPolicyRepository policies;
    private final IncidentRepository incidents;
    private final AuditEntryRepository audit;
    private final StrategyRegistry strategies;
    private final PriorityService priority;
    private final SlaService sla;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Random random = new Random(42);

    public DataSeeder(ItsmProperties props, UserRepository users, ITServiceRepository services, CategoryRepository categories,
                      SlaPolicyRepository policies, IncidentRepository incidents, AuditEntryRepository audit,
                      StrategyRegistry strategies, PriorityService priority, SlaService sla, PasswordEncoder encoder, Clock clock) {
        this.props = props;
        this.users = users;
        this.services = services;
        this.categories = categories;
        this.policies = policies;
        this.incidents = incidents;
        this.audit = audit;
        this.strategies = strategies;
        this.priority = priority;
        this.sla = sla;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.getSeed().isEnabled() || users.count() > 0) return;
        log.info("Seeding demo data...");
        Map<String, ITService> svc = seedServices();
        Map<String, Category> cat = seedCategories();
        seedPolicies(svc);
        Map<String, User> people = seedUsers();
        strategies.update("SMART", "WEIGHTED", true);
        seedHistory(svc, cat, people);
        seedOpenWork(svc, cat, people);
        log.info("Seeded {} users, {} services, {} incidents. Login with any seeded email and password {}",
                users.count(), services.count(), incidents.count(), PASSWORD);
    }

    // ------------------------------------------------------------------ catalogue

    private Map<String, ITService> seedServices() {
        Map<String, ITService> m = new LinkedHashMap<>();
        m.put("NET", new ITService("Network Infrastructure", "Campus LAN, Wi-Fi, WAN links, DNS and firewalls", 5, 2500, "Network Operations"));
        m.put("AUTH", new ITService("Identity & Authentication", "Single sign-on, MFA and directory services", 5, 2500, "Identity Team"));
        m.put("DB", new ITService("Core Database Cluster", "Primary PostgreSQL cluster and read replicas", 5, 1800, "Data Platform"));
        m.put("CLOUD", new ITService("Cloud Platform", "Kubernetes clusters and virtual machines", 4, 1200, "Cloud Engineering"));
        m.put("STORAGE", new ITService("Cloud Storage", "Team shares, object storage and backups", 3, 1500, "Cloud Engineering"));
        m.put("PAY", new ITService("Payment Gateway", "Customer-facing payment processing and refunds", 5, 3000, "Payments Squad"));
        m.put("MAIL", new ITService("Corporate Email", "Exchange mailboxes, calendars and distribution lists", 4, 2500, "Collaboration"));
        m.put("VPN", new ITService("VPN Remote Access", "Remote access for staff working off-site", 4, 900, "Network Operations"));
        m.put("HR", new ITService("HR Portal", "Leave, payroll slips and appraisals", 2, 2500, "Enterprise Apps"));
        m.put("DEV", new ITService("Employee Devices", "Laptops, desktops, monitors, printers and peripherals", 2, 2500, "Desktop Support"));
        services.saveAll(m.values());
        dep(m, "AUTH", "NET");
        dep(m, "DB", "NET");
        dep(m, "CLOUD", "NET");
        dep(m, "STORAGE", "CLOUD", "AUTH");
        dep(m, "PAY", "DB", "AUTH", "CLOUD");
        dep(m, "MAIL", "AUTH", "NET");
        dep(m, "VPN", "AUTH", "NET");
        dep(m, "HR", "AUTH", "DB");
        return m;
    }

    private static void dep(Map<String, ITService> m, String svc, String... on) {
        for (String o : on) m.get(svc).addDependency(m.get(o));
    }

    private Map<String, Category> seedCategories() {
        List<Category> list = List.of(
                new Category("HARDWARE", "Physical devices and peripherals", "HARDWARE",
                        List.of("laptop", "monitor", "keyboard", "mouse", "printer", "battery", "charger", "screen", "power", "hardware", "docking")),
                new Category("SOFTWARE", "Desktop software and licences", "SOFTWARE",
                        List.of("excel", "word", "office", "install", "licence", "license", "update", "crash", "outlook", "teams", "software", "driver")),
                new Category("NETWORK", "Connectivity, Wi-Fi, DNS, VPN", "NETWORK",
                        List.of("network", "wifi", "internet", "dns", "vpn", "router", "switch", "firewall", "latency", "bandwidth", "connectivity", "packet")),
                new Category("DATABASE", "Database availability and performance", "DATABASE",
                        List.of("database", "query", "sql", "replication", "deadlock", "index", "connection pool", "postgres", "table", "replica")),
                new Category("CLOUD", "Cloud compute and platform services", "CLOUD",
                        List.of("cloud", "kubernetes", "pod", "cluster", "vm", "instance", "autoscaling", "container", "deployment", "node")),
                new Category("SECURITY", "Phishing, malware and suspicious activity", "SECURITY",
                        List.of("phishing", "malware", "virus", "suspicious", "breach", "ransomware", "hacked", "security", "spam", "compromised")),
                new Category("AUTHENTICATION", "Login, SSO, MFA and accounts", "AUTHENTICATION",
                        List.of("login", "password", "sso", "mfa", "locked", "account", "authentication", "otp", "credentials", "sign in", "token")),
                new Category("APPLICATION", "Business application faults", "APPLICATION",
                        List.of("application", "app", "portal", "page", "payment", "transaction", "checkout", "refund", "500", "503", "website", "api")),
                new Category("STORAGE", "File shares, quotas and backups", "STORAGE",
                        List.of("storage", "disk", "quota", "share", "drive", "upload", "backup", "file", "bucket", "space"))
        );
        categories.saveAll(list);
        Map<String, Category> m = new HashMap<>();
        list.forEach(c -> m.put(c.getName(), c));
        return m;
    }

    private void seedPolicies(Map<String, ITService> svc) {
        policies.saveAll(List.of(
                new SlaPolicy("Standard P1 - Critical", null, Priority.P1, 5, 30, 75),
                new SlaPolicy("Standard P2 - High", null, Priority.P2, 15, 120, 75),
                new SlaPolicy("Standard P3 - Medium", null, Priority.P3, 60, 480, 80),
                new SlaPolicy("Standard P4 - Low", null, Priority.P4, 240, 1440, 80),
                new SlaPolicy("Payments P1 - Revenue critical", svc.get("PAY"), Priority.P1, 5, 20, 70),
                new SlaPolicy("Payments P2", svc.get("PAY"), Priority.P2, 10, 60, 70)));
    }

    private Map<String, User> seedUsers() {
        String h = encoder.encode(PASSWORD);
        Map<String, User> m = new LinkedHashMap<>();
        m.put("admin", user(Role.ADMIN, "A001", "Riya Kapoor", "admin@itsm.local", h, Set.of(), 0, 0, "+91 90000 00001", "IT Operations"));
        m.put("manager", user(Role.MANAGER, "M001", "Vikram Sethi", "manager@itsm.local", h, Set.of(), 0, 0, "+91 90000 00002", "IT Management"));
        m.put("lead1", user(Role.TEAM_LEAD, "L101", "Meera Iyer", "lead.infra@itsm.local", h, Set.of("NETWORK", "SECURITY", "DATABASE", "AUTHENTICATION"), 11, 4, "+91 90000 00011", "Infrastructure"));
        m.put("lead2", user(Role.TEAM_LEAD, "L102", "Arjun Rao", "lead.apps@itsm.local", h, Set.of("APPLICATION", "SOFTWARE", "CLOUD", "STORAGE"), 9, 4, "+91 90000 00012", "Applications"));
        m.put("t101", user(Role.TECHNICIAN, "T101", "Kabir Malhotra", "tech.network@itsm.local", h, Set.of("NETWORK", "SECURITY"), 6, 5, "+91 90000 00101", "Network Operations"));
        m.put("t102", user(Role.TECHNICIAN, "T102", "Sneha Pillai", "tech.data@itsm.local", h, Set.of("DATABASE", "STORAGE"), 7, 5, "+91 90000 00102", "Data Platform"));
        m.put("t103", user(Role.TECHNICIAN, "T103", "Rohan Das", "tech.apps@itsm.local", h, Set.of("APPLICATION", "SOFTWARE"), 4, 6, "+91 90000 00103", "Enterprise Apps"));
        m.put("t104", user(Role.TECHNICIAN, "T104", "Aisha Khan", "tech.desktop@itsm.local", h, Set.of("HARDWARE", "SOFTWARE"), 3, 6, "+91 90000 00104", "Desktop Support"));
        m.put("t105", user(Role.TECHNICIAN, "T105", "Dev Patel", "tech.identity@itsm.local", h, Set.of("AUTHENTICATION", "SECURITY", "CLOUD"), 5, 5, "+91 90000 00105", "Identity Team"));
        m.put("t106", user(Role.TECHNICIAN, "T106", "Nisha Verma", "tech.cloud@itsm.local", h, Set.of("CLOUD", "STORAGE", "APPLICATION"), 2, 5, "+91 90000 00106", "Cloud Engineering"));
        m.put("e101", user(Role.EMPLOYEE, "E101", "Ananya Sharma", "employee@itsm.local", h, Set.of(), 0, 0, "+91 90000 01001", "Finance"));
        m.put("e102", user(Role.EMPLOYEE, "E102", "Karan Mehta", "karan@itsm.local", h, Set.of(), 0, 0, "+91 90000 01002", "Sales"));
        m.put("e103", user(Role.EMPLOYEE, "E103", "Pooja Nair", "pooja@itsm.local", h, Set.of(), 0, 0, "+91 90000 01003", "Customer Support"));
        m.put("e104", user(Role.EMPLOYEE, "E104", "Siddharth Jain", "siddharth@itsm.local", h, Set.of(), 0, 0, "+91 90000 01004", "Human Resources"));
        users.saveAll(m.values());
        return m;
    }

    private static User user(Role role, String code, String name, String email, String hash, Set<String> skills,
                             int exp, int max, String phone, String dept) {
        User u = UserFactory.create(new UserFactory.UserSpec(role, code, name, email, hash, skills, exp, max == 0 ? null : max));
        u.updateProfile(name, phone, dept);
        if (role.isStaff()) {
            u.setPreferredChannels(EnumSet.of(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL, NotificationChannelType.SMS));
        }
        return u;
    }

    // ------------------------------------------------------------------ incidents

    private record Template(String category, String service, String title, String description, Impact impact, Urgency urgency,
                            int users, String summary, String rootCause, String steps) {}

    private static final List<Template> TEMPLATES = List.of(
            new Template("APPLICATION", "PAY", "Payment application unavailable", "Users are unable to complete transactions; the payment page returns HTTP 503 for every customer.",
                    Impact.EXTENSIVE, Urgency.CRITICAL, 3000, "Restarted payment service pods and scaled out to 6 replicas",
                    "Memory leak in payment-service v2.3 caused pods to crash-loop", "Rolled back to v2.2, raised memory limits, added heap alert"),
            new Template("APPLICATION", "PAY", "Payment transaction failing intermittently", "Around one in five card payments fail with a gateway timeout during checkout.",
                    Impact.HIGH, Urgency.HIGH, 600, "Increased upstream gateway timeout and fixed retry logic",
                    "Retry storm against the card processor after a latency spike", "Added exponential backoff and circuit breaker"),
            new Template("APPLICATION", "PAY", "Refunds stuck in pending state", "Customer refunds initiated yesterday are still showing pending in the payment dashboard.",
                    Impact.MEDIUM, Urgency.MEDIUM, 40, "Re-queued failed refund jobs and fixed the scheduler",
                    "Refund batch scheduler stopped after a config change", "Restored cron expression and replayed the queue"),
            new Template("AUTHENTICATION", "AUTH", "Unable to login with SSO", "The single sign-on page loops back to the login screen after entering credentials. Several colleagues have the same issue.",
                    Impact.HIGH, Urgency.HIGH, 400, "Renewed the expired SAML signing certificate",
                    "SAML signing certificate expired overnight", "Rotated certificate, updated IdP metadata, added expiry monitoring"),
            new Template("AUTHENTICATION", "AUTH", "Account locked after password reset", "My account got locked right after I reset my password and I cannot sign in to anything.",
                    Impact.LOW, Urgency.HIGH, 1, "Unlocked account and cleared cached credentials on the user's devices",
                    "Old password cached on mobile mail client caused repeated failed logins", "Unlocked in directory, removed stale credentials"),
            new Template("AUTHENTICATION", "AUTH", "MFA codes not accepted", "The authenticator OTP codes are always rejected as invalid at login.",
                    Impact.MEDIUM, Urgency.HIGH, 25, "Resynchronised MFA server clock with NTP",
                    "Clock drift on MFA server exceeded the OTP tolerance window", "Fixed NTP configuration and resynced tokens"),
            new Template("NETWORK", "NET", "Wi-Fi keeps disconnecting on 3rd floor", "Office Wi-Fi drops every few minutes on the third floor and video calls keep failing.",
                    Impact.MEDIUM, Urgency.MEDIUM, 80, "Replaced faulty access point AP-3F-02",
                    "Access point firmware crash", "Swapped the access point and updated firmware on the remaining APs"),
            new Template("NETWORK", "NET", "Internet very slow in Block B", "Pages take more than 30 seconds to load and downloads time out for everyone in Block B.",
                    Impact.HIGH, Urgency.MEDIUM, 200, "Moved the backup job off-hours and applied QoS",
                    "Backup job saturating the uplink during business hours", "Rescheduled backup to 02:00 and applied a QoS policy"),
            new Template("NETWORK", "NET", "Cannot reach internal sites - DNS errors", "The browser shows DNS errors for the intranet and HR portal; external sites work.",
                    Impact.HIGH, Urgency.HIGH, 500, "Restarted DNS resolver and removed the stale zone",
                    "Stale DNS zone left behind after server migration", "Cleaned zone records and flushed resolver caches"),
            new Template("NETWORK", "VPN", "VPN disconnects every 10 minutes", "While working from home the VPN drops every ten minutes and I have to reconnect.",
                    Impact.MEDIUM, Urgency.MEDIUM, 60, "Updated VPN client and adjusted idle timeout",
                    "Aggressive idle timeout on the new VPN concentrator", "Raised idle timeout to 60 minutes, pushed client 5.2"),
            new Template("DATABASE", "DB", "Finance report query timing out", "The monthly finance report query fails with a timeout after 60 seconds.",
                    Impact.MEDIUM, Urgency.HIGH, 30, "Added missing index on transactions(created_at)",
                    "Full table scan after a schema change dropped the index", "Recreated index, analysed table, added query plan check to CI"),
            new Template("DATABASE", "DB", "Database connection pool exhausted", "Applications are throwing 'too many connections' errors against the main database.",
                    Impact.EXTENSIVE, Urgency.HIGH, 1500, "Fixed connection leak in billing service and raised pool size",
                    "Connection leak in billing service", "Patched billing service to close connections, increased max_connections"),
            new Template("DATABASE", "DB", "Replication lag on read replica", "Dashboards show data that is 20 minutes old; the read replica seems to be lagging.",
                    Impact.MEDIUM, Urgency.MEDIUM, 100, "Throttled the bulk import and resynced the replica",
                    "Large bulk import saturated replica I/O", "Moved imports to batches, added lag alerting"),
            new Template("CLOUD", "CLOUD", "Kubernetes pods stuck in pending", "New deployments on the production cluster stay in Pending and never start.",
                    Impact.HIGH, Urgency.HIGH, 300, "Added nodes to the node pool and fixed autoscaler limits",
                    "Cluster autoscaler hit its max node count", "Raised node pool limit, added capacity alert"),
            new Template("CLOUD", "CLOUD", "Autoscaling not triggering on cloud cluster", "Traffic spikes do not trigger autoscaling and the app becomes slow at peak times.",
                    Impact.MEDIUM, Urgency.MEDIUM, 200, "Fixed misconfigured autoscaling policy metric",
                    "HPA pointed at a metric that was renamed", "Updated HPA metric name and tested scale-out"),
            new Template("STORAGE", "STORAGE", "Shared drive full - cannot save files", "Getting 'disk quota exceeded' when saving files to the team shared drive.",
                    Impact.MEDIUM, Urgency.HIGH, 45, "Increased quota and archived files older than 2 years",
                    "Team share reached its quota", "Archived old data to cold storage, raised quota by 200 GB"),
            new Template("STORAGE", "STORAGE", "File upload to cloud storage failing", "Uploading files larger than 100 MB to cloud storage fails halfway with an error.",
                    Impact.LOW, Urgency.MEDIUM, 10, "Enabled multipart upload in the storage gateway",
                    "Gateway request size limit too small", "Raised limit and enabled multipart uploads"),
            new Template("SECURITY", "MAIL", "Suspicious phishing email received", "Several employees received an email asking them to reset their password via an unknown link.",
                    Impact.HIGH, Urgency.HIGH, 300, "Blocked sender domain and purged the emails from all mailboxes",
                    "Targeted phishing campaign spoofing IT", "Blocked domain, purged messages, sent awareness notice"),
            new Template("SECURITY", "DEV", "Malware alert on workstation", "Antivirus raised a malware alert on my laptop after opening an attachment.",
                    Impact.LOW, Urgency.CRITICAL, 1, "Isolated and re-imaged the workstation",
                    "Malicious macro in email attachment", "Isolated device, re-imaged, reset user credentials"),
            new Template("SOFTWARE", "MAIL", "Outlook not receiving new emails", "Outlook has not received any new emails since this morning, webmail works fine.",
                    Impact.LOW, Urgency.MEDIUM, 1, "Rebuilt the Outlook profile and cleared OST cache",
                    "Corrupted local OST file", "Created new profile and re-synced mailbox"),
            new Template("SOFTWARE", "DEV", "Excel crashes when opening large files", "Excel closes without warning whenever I open spreadsheets larger than 20 MB.",
                    Impact.LOW, Urgency.LOW, 1, "Updated Office to the latest build and disabled a faulty add-in",
                    "Incompatible third-party add-in", "Removed add-in, updated Office"),
            new Template("HARDWARE", "DEV", "Laptop not powering on", "My laptop does not turn on even when plugged in; no lights at all.",
                    Impact.LOW, Urgency.HIGH, 1, "Replaced faulty charger and battery",
                    "Failed battery and charger", "Swapped battery and charger, tested under load"),
            new Template("HARDWARE", "DEV", "Monitor flickering constantly", "External monitor flickers every few seconds, making it hard to work.",
                    Impact.LOW, Urgency.LOW, 1, "Replaced the HDMI cable and updated display driver",
                    "Damaged HDMI cable", "New cable, driver update"),
            new Template("APPLICATION", "HR", "HR portal leave page shows error", "Applying for leave on the HR portal shows 'Something went wrong' after submitting.",
                    Impact.MEDIUM, Urgency.MEDIUM, 300, "Fixed null pointer in the leave module and redeployed",
                    "Null manager reference for newly joined employees", "Patched leave module, backfilled manager data")
    );

    private void seedHistory(Map<String, ITService> svc, Map<String, Category> cat, Map<String, User> people) {
        Instant now = Instant.now(clock);
        List<User> employees = List.of(people.get("e101"), people.get("e102"), people.get("e103"), people.get("e104"));
        List<Technician> techs = users.findActiveTechnicians();
        int count = 52;
        for (int n = 0; n < count; n++) {
            Template t = TEMPLATES.get((n * 7 + random.nextInt(3)) % TEMPLATES.size());
            Instant created = now.minus(Duration.ofMinutes(60L * 24 * 14 - (long) n * (60 * 24 * 14 - 600) / count))
                    .plus(Duration.ofMinutes(random.nextInt(90)));
            User reporter = employees.get(random.nextInt(employees.size()));
            Incident i = new Incident(t.title(), t.description(), reporter, svc.get(t.service()), t.impact(), t.urgency(),
                    Math.max(1, t.users() + random.nextInt(Math.max(1, t.users() / 5 + 1))), created);
            incidents.save(i);
            log(i, created, reporter, "INCIDENT_CREATED", null, "NEW", i.getNumber() + " created by " + reporter.getFullName() + " (" + reporter.getCode() + ")");

            Category c = cat.get(t.category());
            i.applyCategory(c, true, 0.7 + random.nextInt(25) / 100.0);
            priority.apply(i);
            SlaPolicy p = sla.resolvePolicy(i.getService(), i.getPriority());
            i.applySla(p, p.responseTarget(), p.resolutionTarget()); // history uses real (unscaled) policy time
            i.markClassified(created.plusSeconds(5));
            log(i, created.plusSeconds(5), null, "INCIDENT_CLASSIFIED", "NEW", "CLASSIFIED", "Incident classified as " + c.getName());
            log(i, created.plusSeconds(5), null, "PRIORITY_CALCULATED", null, "CLASSIFIED", "Priority calculated as " + i.getPriority());

            Technician tech = pickTech(techs, c.getRequiredSkill(), n);
            Instant assigned = created.plusSeconds(20);
            i.assignTo(tech, assigned);
            log(i, assigned, null, "INCIDENT_ASSIGNED", "CLASSIFIED", "ASSIGNED", "Assigned to " + tech.getDisplayTitle() + " " + tech.getCode() + " using Smart recommendation strategy");

            boolean breachResponse = random.nextInt(100) < 12;
            long respSecs = p.responseTarget().getSeconds();
            Instant ack = assigned.plusSeconds(breachResponse ? (long) (respSecs * 1.4) : (long) (respSecs * (0.2 + random.nextDouble() * 0.6)));
            i.acknowledge(ack);
            log(i, ack, tech, "INCIDENT_ACKNOWLEDGED", "ASSIGNED", "ACKNOWLEDGED", "Technician acknowledged incident");
            Instant start = ack.plusSeconds(60 + random.nextInt(240));
            i.startWork(start);
            log(i, start, tech, "INCIDENT_WORK_STARTED", "ACKNOWLEDGED", "IN_PROGRESS", "Investigation started");
            i.addNote(tech.getId(), tech.getFullName(), tech.getRole().name(), NoteType.WORK_NOTE, "Investigating: " + t.rootCause().toLowerCase(), start.plusSeconds(120));

            boolean breachResolution = random.nextInt(100) < 10;
            long resSecs = p.resolutionTarget().getSeconds();
            Instant resolved = created.plusSeconds(breachResolution ? (long) (resSecs * 1.3) : (long) (resSecs * (0.3 + random.nextDouble() * 0.55)));
            if (!resolved.isAfter(start)) resolved = start.plusSeconds(300);
            if (resolved.isAfter(now)) resolved = now.minusSeconds(60);
            i.resolve(new Resolution(t.summary(), t.rootCause(), t.steps(), tech.getFullName(), resolved), resolved);
            if (i.getSla().isBreached()) log(i, resolved, null, "SLA_VIOLATED", null, "IN_PROGRESS", "SLA target violated (" + p.getName() + ")");
            log(i, resolved, tech, "INCIDENT_RESOLVED", "IN_PROGRESS", "RESOLVED", "Incident marked RESOLVED: " + t.summary());

            int fate = random.nextInt(100);
            if (fate < 75 && resolved.plusSeconds(3600).isBefore(now)) {
                Instant verified = resolved.plusSeconds(600 + random.nextInt(3000));
                i.verify(verified);
                log(i, verified, reporter, "INCIDENT_VERIFIED", "RESOLVED", "VERIFIED", "Resolution verified by " + reporter.getFullName());
                if (random.nextInt(100) < 70) i.submitFeedback(new Feedback(3 + random.nextInt(3), random.nextBoolean() ? "Quick and helpful, thanks!" : null, verified));
                Instant closed = verified.plusSeconds(300 + random.nextInt(1800));
                if (closed.isBefore(now)) {
                    i.close("Confirmed working with reporter", tech.getFullName(), closed);
                    log(i, closed, tech, "INCIDENT_CLOSED", "VERIFIED", "CLOSED", "Incident CLOSED by " + tech.getFullName());
                }
            }
            i.refreshSlaStatus(now);
        }
    }

    private Technician pickTech(List<Technician> techs, String skill, int n) {
        List<Technician> skilled = techs.stream().filter(t -> t.hasSkill(skill)).toList();
        List<Technician> pool = skilled.isEmpty() ? techs : skilled;
        return pool.get(n % pool.size());
    }

    /** A few live incidents so technician queues are not empty at the start of a demo. */
    private void seedOpenWork(Map<String, ITService> svc, Map<String, Category> cat, Map<String, User> people) {
        Instant now = Instant.now(clock);
        openIncident("Teams screen sharing freezes", "When I share my screen in Teams meetings the video freezes for other participants after a few seconds.",
                svc.get("DEV"), cat.get("SOFTWARE"), Impact.LOW, Urgency.MEDIUM, 1, people.get("e102"),
                (Technician) people.get("t104"), now.minusSeconds(240), IncidentStatus.IN_PROGRESS);
        openIncident("HR portal payslip download is blank", "Downloading the October payslip from the HR portal gives an empty PDF.",
                svc.get("HR"), cat.get("APPLICATION"), Impact.MEDIUM, Urgency.LOW, 50, people.get("e101"),
                (Technician) people.get("t103"), now.minusSeconds(420), IncidentStatus.AWAITING_INFO);
        openIncident("Docking station not detecting monitors", "My docking station stopped detecting both external monitors after the latest update.",
                svc.get("DEV"), cat.get("HARDWARE"), Impact.LOW, Urgency.LOW, 1, people.get("e103"),
                (Technician) people.get("t104"), now.minusSeconds(90), IncidentStatus.ASSIGNED);
        openIncident("Nightly backup job failed", "The nightly backup of the team share reported failure with 'insufficient space' in the job log.",
                svc.get("STORAGE"), cat.get("STORAGE"), Impact.MEDIUM, Urgency.MEDIUM, 20, people.get("e104"),
                (Technician) people.get("t102"), now.minusSeconds(150), IncidentStatus.ACKNOWLEDGED);
    }

    private void openIncident(String title, String desc, ITService service, Category c, Impact impact, Urgency urgency, int affected,
                              User reporter, Technician tech, Instant created, IncidentStatus target) {
        Incident i = new Incident(title, desc, reporter, service, impact, urgency, affected, created);
        incidents.save(i);
        log(i, created, reporter, "INCIDENT_CREATED", null, "NEW", i.getNumber() + " created by " + reporter.getFullName() + " (" + reporter.getCode() + ")");
        i.applyCategory(c, true, 0.82);
        priority.apply(i);
        sla.apply(i);
        i.markClassified(created.plusSeconds(2));
        log(i, created.plusSeconds(2), null, "INCIDENT_CLASSIFIED", "NEW", "CLASSIFIED", "Incident classified as " + c.getName());
        log(i, created.plusSeconds(2), null, "PRIORITY_CALCULATED", null, "CLASSIFIED", "Priority calculated as " + i.getPriority());
        i.assignTo(tech, created.plusSeconds(3));
        log(i, created.plusSeconds(3), null, "INCIDENT_ASSIGNED", "CLASSIFIED", "ASSIGNED", "Assigned to " + tech.getDisplayTitle() + " " + tech.getCode() + " using Smart recommendation strategy");
        if (target == IncidentStatus.ASSIGNED) { i.refreshSlaStatus(Instant.now(clock)); return; }
        i.acknowledge(created.plusSeconds(30));
        log(i, created.plusSeconds(30), tech, "INCIDENT_ACKNOWLEDGED", "ASSIGNED", "ACKNOWLEDGED", "Technician acknowledged incident");
        if (target == IncidentStatus.ACKNOWLEDGED) { i.refreshSlaStatus(Instant.now(clock)); return; }
        i.startWork(created.plusSeconds(60));
        log(i, created.plusSeconds(60), tech, "INCIDENT_WORK_STARTED", "ACKNOWLEDGED", "IN_PROGRESS", "Investigation started");
        if (target == IncidentStatus.AWAITING_INFO) {
            i.requestInfo(created.plusSeconds(120));
            i.addNote(tech.getId(), tech.getFullName(), tech.getRole().name(), NoteType.INFO_REQUEST,
                    "Could you tell us which browser you are using and attach a screenshot of the blank PDF?", created.plusSeconds(120));
            log(i, created.plusSeconds(120), tech, "INCIDENT_INFO_REQUESTED", "IN_PROGRESS", "AWAITING_INFO", "Additional information requested from reporter (SLA paused)");
        }
        i.refreshSlaStatus(Instant.now(clock));
    }

    private void log(Incident i, Instant at, User actor, String action, String prev, String next, String desc) {
        audit.save(new AuditEntry(at, actor == null ? null : actor.getId(),
                actor == null ? "SYSTEM" : actor.getFullName() + " (" + actor.getCode() + ")",
                action, i.getId(), i.getNumber(), prev, next, desc));
    }
}

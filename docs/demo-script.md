# Demo video script (15–20 min)

**File name:** `<GroupNo>_OOP_S1_2025-2026_Project` (as specified in the brief). Every member appears on camera, introduces themselves (name + ID) and explains the module(s) they own.

## Before recording (5 min setup)

1. Start the stack with `docker compose up --build`, or use the deployed URL. Open `http://localhost:8080`.
2. Open **four browser windows** (use separate profiles or incognito so sessions don't clash), each signed in with password `Password@123`:
   - **A** Employee: `employee@itsm.local`
   - **B** Technician: whoever the new incident gets assigned to. Start with `lead.apps@itsm.local`; the Smart strategy usually routes payment/application incidents to L102 Arjun Rao or T103 Rohan Das.
   - **C** Admin: `admin@itsm.local`, left on **Dashboard**
   - **D** IT Manager: `manager@itsm.local`, used for the escalation finale
3. The SLA demo clock runs ×10 (`SLA_TIME_SCALE=10`), so a P1 response target of 5 min becomes 30 s and escalations happen on camera.
4. If you want a clean database: `docker compose down -v && docker compose up --build`.

## 0. Introduction (2 min) — Member 3

- Team introductions, each person names their module (see report §13).
- The problem: incidents handled by email lead to delays, wrong priorities, SLA misses and duplicates.
- Architecture slide (report §3): React SPA → Spring Boot (domain, strategies, events, adapters) → PostgreSQL, plus WebSocket for live updates.

## 1. Workflow 1 — Report → prioritise → assign → notify (4 min) — Member 1 and Member 3

1. **A (employee)**: *Report incident*. Type the title "Payment page down for customers" and the description "Customers get HTTP 503 at checkout, all transactions failing since 10:00".
   - Point at **Live analysis**:
     - suggested category *application* with its confidence
     - predicted **P1** with the priority rationale formula
     - SLA targets
     - *possibly already reported* (duplicate detection)
     - *known fixes*
2. Choose service **Payment Gateway**, impact **Everyone**, urgency **Critical**, 1500 users. Submit.
3. Walk through the **submission summary**: Created → Classified → Prioritised → SLA → Assigned → Notified.
4. **C (admin dashboard)**: the **Live activity** feed has updated without a refresh (WebSocket). The KPI tiles change.
5. **B (technician)**: a toast notification appears, the bell shows a red badge, and the incident is at the top of **My queue** with a red countdown.
6. Member 3 explains the code behind this:
   - `IncidentService.create` orchestrates the steps
   - `IncidentFactory` builds the incident (Factory)
   - `WeightedScoringPriorityStrategy` calculates priority (Strategy)
   - `SmartRecommendationStrategy` picks the technician (Strategy)
   - `IncidentEventPublisher` fans out to the audit, notification and dashboard listeners (Observer)

## 2. Workflow 2 — Investigate → SLA monitored → resolve → user notified (4 min) — Member 2 and Member 4

1. **B**: open the incident.
   - Show the **SLA panel** with its countdown and progress bar.
   - Click **Acknowledge**. The response SLA is met; show the green tick.
   - Click **Start work**.
2. **B**: add an **internal note** (toggle on). Then click **Request info**: "Which payment methods fail?". The status becomes *Awaiting info* and the SLA shows **Paused**.
3. **A**: an amber banner appears. Click **Reply**: "All card payments". The investigation resumes and the resolution deadline is extended by the paused time.
4. **B**: click **Resolve**. Pick a **suggested fix** chip, which pre-fills the form, then submit. The SLA status shows *Met*.
5. **A**: a toast arrives. Click **Confirm fixed**, then **Rate** 5 stars.
6. **B**: click **Close**.
7. Open the **Audit trail** tab: every step has a timestamp, user, action and previous → new state.
8. Member 2 demonstrates the security rules:
   - The employee has no Resolve button.
   - Show an API call in Swagger (`/swagger-ui.html`) with the employee token: `POST /incidents/{id}/close` returns **409 INVALID_STATE_TRANSITION**, and `GET /admin/users` returns **403**.
9. Member 4 shows the database. Run `docker compose exec db psql -U itsm -d itsm -c "select id, title, status, priority, sla_status from incidents order by id desc limit 5"`, or show the ER diagram. Point out the single-table inheritance (`user_type`) and the embedded SLA columns.

## 3. Workflow 3 — Multiple reports → duplicates/correlation → escalation (4 min) — Member 5

1. Report **three incidents** within about 30 s. Use windows A, plus `karan@itsm.local` and `pooja@itsm.local`, or the quick script in `docs/demo-workflow3.sh`:
   - "Cannot login to email – SSO error" on Corporate Email, impact High, urgency Critical
   - "VPN authentication failing for everyone" on VPN Remote Access
   - "Payment portal login page rejects credentials" on Payment Gateway
2. **C (dashboard)**: wait for the next monitor cycle (10 s), or click **Run SLA check**. The **Correlated incidents** card shows *Suspected root cause: Identity & Authentication*, flagged as a major incident. Explain that the dependency graph is walked with a breadth-first search.
3. Open one of the incidents: the **Similar incidents** panel shows the related reports. Use **Link duplicate** to link one to the master. Mention that duplicates are resolved together with their master.
4. Do **not** acknowledge. Within roughly 30–60 s:
   - an **SLA approaching** warning appears, then **SLA violated**
   - the incident is **reassigned to a team lead** (escalation level 1)
   - administrators are notified (level 2), then the **IT Manager** (window D, level 3)
   - toasts appear in each window
5. Show the **Notification log**: in-app, e-mail and SMS deliveries through the adapters, with SMS reserved for urgent events. Show the audit entries for `INCIDENT_ESCALATED`.
6. Member 5 shows the **test suite**: run `./mvnw test`, which reports 57 tests passing, and the GitHub Actions CI run.

## 4. Administration & extensibility (2 min) — Member 4 and Member 1

- **Strategies** page: switch assignment from *Smart* to *Round-robin*, create an incident, and show the different routing. This is the Strategy pattern working at runtime.
- **Categories**: add a new category, e.g. `PRINTING` with keywords such as *printer, toner*. Report "Printer out of toner" and it is auto-categorised with no code change.
- **SLA policies**: show the service-specific Payments policy overriding the default.
- **Users**: change a technician's skills or capacity, or toggle their availability.
- Toggle **dark mode** and show the mobile layout.

## 5. Wrap-up (1 min) — Member 3

- Summarise the OOP concepts and the 8 patterns (report §5), plus the 6 intelligent features and the innovation items.
- Limitations and future work (report §14).

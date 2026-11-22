package com.itsm.service;

import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Search filters (10 criteria): number, keyword, status, priority, category, service,
 * technician, SLA status, date range, plus scoping flags.
 */
public record IncidentSearchCriteria(String number, String q, List<IncidentStatus> status, List<Priority> priority,
                                     Long categoryId, Long serviceId, Long technicianId, Long reporterId,
                                     SlaStatus slaStatus, LocalDate from, LocalDate to,
                                     Boolean openOnly, Boolean unassigned, Boolean majorOnly, Boolean mine) {}

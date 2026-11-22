package com.itsm.assignment;

import com.itsm.domain.user.Technician;

import java.util.Map;

/** A ranked candidate with an explainable score breakdown. */
public record TechnicianScore(Technician technician, double score, int workload,
                              Map<String, Double> breakdown, String reason) {}

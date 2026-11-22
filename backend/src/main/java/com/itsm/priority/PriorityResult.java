package com.itsm.priority;

import com.itsm.domain.incident.Priority;

public record PriorityResult(Priority priority, double score, String rationale) {}

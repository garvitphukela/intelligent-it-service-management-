package com.itsm.event;

/** Who should be told about an event. Resolved to concrete users by the notification service. */
public enum Audience { REPORTER, ASSIGNEE, PREVIOUS_ASSIGNEE, TEAM_LEADS, ADMINS, MANAGERS }

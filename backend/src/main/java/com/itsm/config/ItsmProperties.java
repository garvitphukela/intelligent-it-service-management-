package com.itsm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly-typed application configuration bound from {@code itsm.*}. */
@ConfigurationProperties(prefix = "itsm")
public class ItsmProperties {

    private final Jwt jwt = new Jwt();
    private final Sla sla = new Sla();
    private final Notifications notifications = new Notifications();
    private final Intelligence intelligence = new Intelligence();
    private final Seed seed = new Seed();
    private String corsOrigins = "http://localhost:5173";

    public Jwt getJwt() { return jwt; }
    public Sla getSla() { return sla; }
    public Notifications getNotifications() { return notifications; }
    public Intelligence getIntelligence() { return intelligence; }
    public Seed getSeed() { return seed; }
    public String getCorsOrigins() { return corsOrigins; }
    public void setCorsOrigins(String corsOrigins) { this.corsOrigins = corsOrigins; }

    public static class Jwt {
        private String secret = "change-me-change-me-change-me-change-me-change-me-change-me";
        private long expirationMinutes = 720;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMinutes() { return expirationMinutes; }
        public void setExpirationMinutes(long expirationMinutes) { this.expirationMinutes = expirationMinutes; }
    }

    public static class Sla {
        /** Policy minutes are divided by this factor (e.g. 10 => a 30 min target lasts 3 real minutes). */
        private double timeScale = 1.0;
        private long monitorIntervalMs = 15000;
        public double getTimeScale() { return timeScale; }
        public void setTimeScale(double timeScale) { this.timeScale = timeScale <= 0 ? 1.0 : timeScale; }
        public long getMonitorIntervalMs() { return monitorIntervalMs; }
        public void setMonitorIntervalMs(long monitorIntervalMs) { this.monitorIntervalMs = monitorIntervalMs; }
    }

    public static class Notifications {
        private boolean emailEnabled = false;
        private String emailFrom = "itsm-noreply@example.com";
        public boolean isEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
        public String getEmailFrom() { return emailFrom; }
        public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }
    }

    public static class Intelligence {
        private double duplicateThreshold = 0.35;
        private double likelyDuplicateThreshold = 0.6;
        private int duplicateWindowDays = 7;
        private int correlationWindowMinutes = 120;
        private int correlationMinIncidents = 3;
        public double getDuplicateThreshold() { return duplicateThreshold; }
        public void setDuplicateThreshold(double v) { this.duplicateThreshold = v; }
        public double getLikelyDuplicateThreshold() { return likelyDuplicateThreshold; }
        public void setLikelyDuplicateThreshold(double v) { this.likelyDuplicateThreshold = v; }
        public int getDuplicateWindowDays() { return duplicateWindowDays; }
        public void setDuplicateWindowDays(int v) { this.duplicateWindowDays = v; }
        public int getCorrelationWindowMinutes() { return correlationWindowMinutes; }
        public void setCorrelationWindowMinutes(int v) { this.correlationWindowMinutes = v; }
        public int getCorrelationMinIncidents() { return correlationMinIncidents; }
        public void setCorrelationMinIncidents(int v) { this.correlationMinIncidents = v; }
    }

    public static class Seed {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

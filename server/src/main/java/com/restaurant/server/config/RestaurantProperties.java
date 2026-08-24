package com.restaurant.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Server-side configuration. Bound from `restaurant.*` in application.yml.
 */
@ConfigurationProperties(prefix = "restaurant")
public class RestaurantProperties {

    private String dataDir;
    private String uploadsDir;
    private String backupsDir;
    private String logsDir;
    private String configDir;

    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();
    private Backup backup = new Backup();
    private Fcm fcm = new Fcm();

    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getUploadsDir() { return uploadsDir; }
    public void setUploadsDir(String uploadsDir) { this.uploadsDir = uploadsDir; }
    public String getBackupsDir() { return backupsDir; }
    public void setBackupsDir(String backupsDir) { this.backupsDir = backupsDir; }
    public String getLogsDir() { return logsDir; }
    public void setLogsDir(String logsDir) { this.logsDir = logsDir; }
    public String getConfigDir() { return configDir; }
    public void setConfigDir(String configDir) { this.configDir = configDir; }
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Backup getBackup() { return backup; }
    public void setBackup(Backup backup) { this.backup = backup; }
    public Fcm getFcm() { return fcm; }
    public void setFcm(Fcm fcm) { this.fcm = fcm; }

    public static class Jwt {
        private String issuer = "restaurant-server";
        private long expirationHours = 12;

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public long getExpirationHours() { return expirationHours; }
        public void setExpirationHours(long expirationHours) { this.expirationHours = expirationHours; }
        public Duration getExpirationDuration() { return Duration.ofHours(expirationHours); }
    }

    public static class RateLimit {
        private Login login = new Login();

        public Login getLogin() { return login; }
        public void setLogin(Login login) { this.login = login; }

        public static class Login {
            private int attempts = 5;
            private int windowMinutes = 15;

            public int getAttempts() { return attempts; }
            public void setAttempts(int attempts) { this.attempts = attempts; }
            public int getWindowMinutes() { return windowMinutes; }
            public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
        }
    }

    public static class Backup {
        private int retentionDaily = 30;
        private int retentionWeekly = 12;
        private String scheduleCron = "0 0 2 * * *";
        private String timezone = "Asia/Ho_Chi_Minh";

        public int getRetentionDaily() { return retentionDaily; }
        public void setRetentionDaily(int retentionDaily) { this.retentionDaily = retentionDaily; }
        public int getRetentionWeekly() { return retentionWeekly; }
        public void setRetentionWeekly(int retentionWeekly) { this.retentionWeekly = retentionWeekly; }
        public String getScheduleCron() { return scheduleCron; }
        public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    /**
     * V2.3 FCM push notification settings. Disabled by default; flip on in
     * application.yml (or via env vars) only after the service-account JSON
     * is provisioned. {@code dryRun=true} logs the payload but does not call
     * Firebase — safe to leave on in development.
     */
    public static class Fcm {
        private boolean enabled = false;
        private String projectId = "";
        /** Absolute path or path relative to {@code restaurant.configDir}. */
        private String credentialsPath = "firebase-service-account.json";
        private boolean dryRun = true;
        /** Auto-deactivate tokens not seen for this many days. */
        private int staleAfterDays = 180;
        /**
         * Phase E — Push retry tuning. The retry job will re-attempt FAILED
         * push events up to {@code retryMaxAttempts} times per notification.
         * Per sweep, at most {@code retryMaxPerSweep} events are retried
         * (protects against backlog storms).
         */
        private int retryMaxAttempts = 5;
        private int retryMaxPerSweep = 50;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getCredentialsPath() { return credentialsPath; }
        public void setCredentialsPath(String credentialsPath) { this.credentialsPath = credentialsPath; }
        public boolean isDryRun() { return dryRun; }
        public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
        public int getStaleAfterDays() { return staleAfterDays; }
        public void setStaleAfterDays(int staleAfterDays) { this.staleAfterDays = staleAfterDays; }
        public int getRetryMaxAttempts() { return retryMaxAttempts; }
        public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
        public int getRetryMaxPerSweep() { return retryMaxPerSweep; }
        public void setRetryMaxPerSweep(int retryMaxPerSweep) { this.retryMaxPerSweep = retryMaxPerSweep; }
    }
}
package edu.cit.abella.channel;

class HeartbeatRequest {
    public String appName;
    public String startedAt; // ISO-8601
    public long uptimeSeconds;

    HeartbeatRequest(String appName, String startedAt, long uptimeSeconds) {
        this.appName = appName;
        this.startedAt = startedAt;
        this.uptimeSeconds = uptimeSeconds;
    }
}

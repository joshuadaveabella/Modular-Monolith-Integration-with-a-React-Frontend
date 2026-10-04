package edu.cit.abella.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class HeartbeatResponse {
    public String serverTime;
    public Integer nextHeartbeatSeconds;
}

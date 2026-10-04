package edu.cit.abella.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// One flexible shape for both ORDER_PLACED and ORDER_CANCELLED events -
// fields irrelevant to a given type are simply null for that event.
@JsonIgnoreProperties(ignoreUnknown = true)
class FeedEvent {
    public Long seq;
    public String eventId;
    public String type; // "ORDER_PLACED" | "ORDER_CANCELLED"
    public String orderId;
    public String placedAt;
    public String decisionDeadline;
    public List<FeedLine> lines;
    public String cancelledAt;
    public String confirmDeadline;
}

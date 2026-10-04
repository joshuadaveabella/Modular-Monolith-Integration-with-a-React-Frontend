package edu.cit.abella.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class FeedResponse {
    public List<FeedEvent> events;
    public Long nextCursor;
}

package edu.cit.abella.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class FeedLine {
    public String sellerSku;
    public Integer qty;
}

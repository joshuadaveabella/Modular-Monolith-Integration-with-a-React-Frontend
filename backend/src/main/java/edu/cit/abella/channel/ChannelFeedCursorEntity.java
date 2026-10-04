package edu.cit.abella.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Single-row table. Persisting the cursor (rather than holding it only in
// memory) is what makes "picks up every order it missed after a restart"
// true - a fresh JVM reads this row instead of starting from nothing.
@Entity
@Table(name = "channel_feed_cursor")
class ChannelFeedCursorEntity {

    @Id
    private Long id; // always 1

    @Column(name = "last_seq")
    private Long lastSeq; // null until the first poll ever succeeds

    protected ChannelFeedCursorEntity() {
    }

    ChannelFeedCursorEntity(Long id) {
        this.id = id;
    }

    Long getId() { return id; }
    Long getLastSeq() { return lastSeq; }
    void setLastSeq(Long lastSeq) { this.lastSeq = lastSeq; }
}

package edu.cit.abella.channel;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChannelProcessedEventRepository extends JpaRepository<ChannelProcessedEventEntity, String> {
    boolean existsByEventId(String eventId);
}

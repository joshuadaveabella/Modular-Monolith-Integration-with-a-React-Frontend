package edu.cit.abella.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ChannelOrderMappingRepository extends JpaRepository<ChannelOrderMappingEntity, String> {
    Optional<ChannelOrderMappingEntity> findByShopOrderId(Long shopOrderId);
}

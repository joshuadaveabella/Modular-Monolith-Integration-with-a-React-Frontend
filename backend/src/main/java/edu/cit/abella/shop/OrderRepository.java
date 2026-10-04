package edu.cit.abella.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findAllByOrderByOrderIdDesc();

    // Used to find BACKORDERED orders waiting on a specific product when a
    // supplier delivery arrives (Task 6).
    List<OrderEntity> findAllByStatus(String status);
}

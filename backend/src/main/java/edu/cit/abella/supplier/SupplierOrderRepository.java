package edu.cit.abella.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SupplierOrderRepository extends JpaRepository<SupplierOrderEntity, Long> {

    List<SupplierOrderEntity> findAllByStatus(SupplierOrderStatus status);

    List<SupplierOrderEntity> findAllByStatusIn(List<SupplierOrderStatus> statuses);

    Optional<SupplierOrderEntity> findByBuyerRef(String buyerRef);

    // Used by hasOpenOrder() - "a purchase order for that product is
    // already on its way" (Task 6).
    List<SupplierOrderEntity> findAllByProductIdAndStatusIn(String productId, List<SupplierOrderStatus> statuses);
}

package edu.cit.abella.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SupplierOrderRepository extends JpaRepository<SupplierOrderEntity, Long> {

    List<SupplierOrderEntity> findAllByStatus(SupplierOrderStatus status);

    List<SupplierOrderEntity> findAllByStatusIn(List<SupplierOrderStatus> statuses);

    Optional<SupplierOrderEntity> findByBuyerRef(String buyerRef);
}

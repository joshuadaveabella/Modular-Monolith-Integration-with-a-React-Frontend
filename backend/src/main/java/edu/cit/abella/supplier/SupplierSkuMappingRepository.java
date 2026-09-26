package edu.cit.abella.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SupplierSkuMappingRepository extends JpaRepository<SupplierSkuMappingEntity, String> {
    Optional<SupplierSkuMappingEntity> findByProductId(String productId);
}

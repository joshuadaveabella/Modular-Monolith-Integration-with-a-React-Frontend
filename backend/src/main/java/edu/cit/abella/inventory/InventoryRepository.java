package edu.cit.abella.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

// Package-private, same reasoning as InventoryEntity.
interface InventoryRepository extends JpaRepository<InventoryEntity, String> {
}

package edu.cit.abella.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

// Package-private, same reasoning as InventoryEntity - this is an
// implementation detail of how the inventory module talks to its own table.
interface InventoryRepository extends JpaRepository<InventoryEntity, String> {
}

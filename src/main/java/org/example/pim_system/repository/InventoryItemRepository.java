package org.example.pim_system.repository;

import org.example.pim_system.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByItemCode(String itemCode);
    List<InventoryItem> findAllByOrderByItemNameAsc();
    List<InventoryItem> findByCategory(String category);
    boolean existsByCategoryIgnoreCaseAndItemNameIgnoreCase(String category, String itemName);
    List<InventoryItem> findByCurrentStockLessThanEqual(Integer stockLevel);
    
    // Used for generating the next sequential item code
    Optional<InventoryItem> findTopByOrderByItemCodeDesc();

    // Used for generating the next sequential item code within a category
    Optional<InventoryItem> findTopByCategoryOrderByItemCodeDesc(String category);
}




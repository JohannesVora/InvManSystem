package com.invman.common.repository;

import com.invman.common.entity.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {

    List<ProductComponent> findBySalesProductId(Long salesProductId);

    boolean existsBySalesProductIdAndInventoryItemId(Long salesProductId, Long inventoryItemId);
}

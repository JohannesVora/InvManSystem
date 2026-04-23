package com.invman.common.repository;

import com.invman.common.entity.SupplierOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierOrderLineRepository extends JpaRepository<SupplierOrderLine, Long> {
}

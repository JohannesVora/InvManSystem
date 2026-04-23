package com.invman.common.repository;

import com.invman.common.entity.ReplenishmentOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplenishmentOrderLineRepository extends JpaRepository<ReplenishmentOrderLine, Long> {
}

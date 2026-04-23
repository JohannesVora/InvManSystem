package com.invman.common.repository;

import com.invman.common.entity.ReplenishmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplenishmentOrderRepository extends JpaRepository<ReplenishmentOrder, Long> {
}

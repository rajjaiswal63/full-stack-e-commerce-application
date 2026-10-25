package org.ecommerce.project.repository;

import org.ecommerce.project.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductProductId(Long productId);
}

package org.ecommerce.project.repository;

import org.ecommerce.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByEmailIgnoreCase(String email, Pageable pageable);
    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    Double getTotalRevenue();

    @Query(value = "select distinct o from Order o join o.orderItems item where item.product.user.userId = :sellerId",
            countQuery = "select count(distinct o) from Order o join o.orderItems item where item.product.user.userId = :sellerId")
    Page<Order> findSellerOrders(@Param("sellerId") Long sellerId, Pageable pageable);
}

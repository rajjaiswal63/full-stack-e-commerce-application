package org.ecommerce.project.repository;

import org.ecommerce.project.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select ci from CartItem ci where ci.cart.cartId = :cartId and ci.product.productId = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Modifying
    @Query("delete from CartItem ci where ci.cart.cartId = :cartId and ci.product.productId = :productId")
    int deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Modifying
    @Query("delete from CartItem ci where ci.cart.cartId = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);
}

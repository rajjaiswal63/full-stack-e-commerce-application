package org.ecommerce.project.repository;

import org.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("select c from Cart c left join fetch c.cartItems items where c.user.email = ?1")
    Cart findCartByEmail(String email);

    @Query("select c from Cart c left join fetch c.cartItems items where c.user.email = ?1 and c.cartId = ?2")
    Cart findCartByEmailAndCartId(String email, Long cartId);

    @Query("select distinct c from Cart c join fetch c.cartItems item join fetch item.product p where p.productId = ?1")
    List<Cart> findCartsByProductId(Long productId);

    Optional<Cart> findByUserUserId(Long userId);
}


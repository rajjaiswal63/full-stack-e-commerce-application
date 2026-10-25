package org.ecommerce.project.repository;

import org.ecommerce.project.model.Category;
import org.ecommerce.project.model.Product;
import org.ecommerce.project.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByCategoryOrderByPriceAsc(Category category, Pageable pageable);
    Page<Product> findByCategory(Category category, Pageable pageable);
    Page<Product> findByProductNameLikeIgnoreCase(String keyword, Pageable pageable);
    Page<Product> findByUser(User user, Pageable pageable);
    boolean existsByProductNameIgnoreCaseAndCategoryCategoryId(String productName, Long categoryId);
    boolean existsByCategoryCategoryId(Long categoryId);
}

package org.ecommerce.project.repository;

import org.ecommerce.project.model.Category;
import org.ecommerce.project.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByCategoryOrderByPriceAsc(Category category);

    List<Product> findByProductNameLikeIgnoreCase(String keyword);
}

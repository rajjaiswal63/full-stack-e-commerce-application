package org.ecommerce.project.service;

import org.ecommerce.project.payload.ProductDTO;
import org.ecommerce.project.payload.ProductResponse;

public interface ProductService {

    ProductDTO addProduct(Long categoryId, ProductDTO product);
    ProductResponse getAllProducts();
    ProductResponse getProductsByCategoryId(Long categoryId);
    ProductResponse getProductsByKeyword(String keyword);

    ProductDTO updateproduct(ProductDTO productDTO, Long productId);
}

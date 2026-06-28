package org.ecommerce.project.service;

import org.ecommerce.project.expectionsHandler.ResourceNotFoundException;
import org.ecommerce.project.model.Category;
import org.ecommerce.project.model.Product;
import org.ecommerce.project.payload.ProductDTO;
import org.ecommerce.project.payload.ProductResponse;
import org.ecommerce.project.repository.CategoryRepository;
import org.ecommerce.project.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ModelMapper modelMapper;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category= categoryRepository.findById(categoryId)
                .orElseThrow( ()->new ResourceNotFoundException("category","categoryId",categoryId));
        Product product= modelMapper.map(productDTO, Product.class);
        product.setImage("default.png");
        product.setCategory(category);
        double specialPrice= product.getPrice() - ((product.getDiscount()*0.01)* product.getPrice());
        product.setSpecialPrice(specialPrice);
        Product savedProduct= productRepository.save(product);
        return modelMapper.map(savedProduct,ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProducts() {
        List<Product> products=productRepository.findAll();
        List<ProductDTO> productDTOS= products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setProductDetails(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByCategoryId(Long categoryId) {
        Category category= categoryRepository.findById(categoryId)
                .orElseThrow( ()->new ResourceNotFoundException("category","categoryId",categoryId));
        List<Product> products=productRepository.findByCategoryOrderByPriceAsc(category);
        List<ProductDTO> productDTOS= products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setProductDetails(productDTOS);
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByKeyword(String keyword) {
        List<Product> products=productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%');
        List<ProductDTO> productDTOS= products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        ProductResponse productResponse=new ProductResponse();
        productResponse.setProductDetails(productDTOS);
        return productResponse;
    }

    @Override
    public ProductDTO updateproduct(ProductDTO productDTO, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow( ()->new ResourceNotFoundException("product","productId",productId));
        product.setProductName(productDTO.getProductName());
        product.setDescription(productDTO.getDescription());
        product.setQuantity(productDTO.getQuantity());
        product.setPrice(productDTO.getPrice());
        product.setDiscount(productDTO.getDiscount());
        Product savedProduct= productRepository.save(product);
        return modelMapper.map(savedProduct,ProductDTO.class);

    }

}

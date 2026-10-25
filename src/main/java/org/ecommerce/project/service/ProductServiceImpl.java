package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.ecommerce.project.model.Cart;
import org.ecommerce.project.model.Category;
import org.ecommerce.project.model.Product;
import org.ecommerce.project.model.User;
import org.ecommerce.project.payload.CartDTO;
import org.ecommerce.project.payload.ProductDTO;
import org.ecommerce.project.payload.ProductResponse;
import org.ecommerce.project.repository.CartRepository;
import org.ecommerce.project.repository.CategoryRepository;
import org.ecommerce.project.repository.ProductRepository;
import org.ecommerce.project.repository.OrderItemRepository;
import org.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Value("${project.image}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        if (productRepository.existsByProductNameIgnoreCaseAndCategoryCategoryId(
                productDTO.getProductName().trim(), categoryId)) {
            throw new APIException("Product already exists in this category.");
        }
        validateProduct(productDTO);
        Product product = modelMapper.map(productDTO, Product.class);
        product.setProductId(null);
        product.setImage("default.svg");
        product.setCategory(category);
        product.setUser(authUtil.loggedInUser());
        product.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));
        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Sort sortByAndOrder = buildSort(sortBy, sortOrder);

        validatePaging(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product> pageProducts = productRepository.findAll(spec, pageDetails);

        List<Product> products = pageProducts.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }


    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = buildSort(sortBy, sortOrder);

        validatePaging(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findAll(pageDetails);

        List<Product> products = pageProducts.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = buildSort(sortBy, sortOrder);

        validatePaging(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        User user = authUtil.loggedInUser();
        Page<Product> pageProducts = productRepository.findByUser(user, pageDetails);

        List<Product> products = pageProducts.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByAndOrder = buildSort(sortBy, sortOrder);

        validatePaging(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByCategory(category, pageDetails);

        List<Product> products = pageProducts.getContent();

        if(products.isEmpty()){
            throw new APIException(category.getCategoryName() + " category does not have any products");
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO dto = modelMapper.map(product, ProductDTO.class);
                    dto.setImage(constructImageUrl(product.getImage()));
                    return dto;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = buildSort(sortBy, sortOrder);

        validatePaging(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageDetails);

        List<Product> products = pageProducts.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO dto = modelMapper.map(product, ProductDTO.class);
                    dto.setImage(constructImageUrl(product.getImage()));
                    return dto;
                })
                .toList();

        if(products.isEmpty()){
            throw new APIException("Products not found with keyword: " + keyword);
        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        assertCanManage(productFromDb);
        validateProduct(productDTO);
        Product product = modelMapper.map(productDTO, Product.class);

        productFromDb.setProductName(product.getProductName().trim());
        productFromDb.setDescription(product.getDescription().trim());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setSpecialPrice(calculateSpecialPrice(product.getPrice(), product.getDiscount()));

        Product savedProduct = productRepository.save(productFromDb);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());

            cartDTO.setProducts(products);

            return cartDTO;

        }).collect(Collectors.toList());

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        assertCanManage(product);
        if (orderItemRepository.existsByProductProductId(productId)) {
            throw new APIException("Products in completed orders cannot be deleted.");
        }
        cartService.removeProductFromAllCarts(productId);
        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        assertCanManage(productFromDb);
        String fileName = fileService.uploadImage(path, image);
        productFromDb.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }



    private Sort buildSort(String sortBy, String sortOrder) {
        Set<String> allowed = Set.of("productId", "productName", "price", "quantity", "discount", "specialPrice");
        String field = sortBy != null && allowed.contains(sortBy) ? sortBy : "productId";
        return "desc".equalsIgnoreCase(sortOrder) ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    private void validatePaging(Integer pageNumber, Integer pageSize) {
        if (pageNumber == null || pageNumber < 0 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new APIException("pageNumber must be non-negative and pageSize must be between 1 and 100.");
        }
    }
    private void validateProduct(ProductDTO product) {
        if (product.getProductName() == null || product.getProductName().isBlank()
                || product.getDescription() == null || product.getDescription().isBlank()
                || product.getQuantity() == null || product.getQuantity() < 0
                || !Double.isFinite(product.getPrice()) || product.getPrice() <= 0
                || !Double.isFinite(product.getDiscount()) || product.getDiscount() < 0 || product.getDiscount() > 100) {
            throw new APIException("Provide a product name, description, non-negative stock, positive price, and discount between 0 and 100.");
        }
    }

    private double calculateSpecialPrice(double price, double discount) {
        return Math.round(price * (1 - discount / 100.0) * 100.0) / 100.0;
    }

    private void assertCanManage(Product product) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (product.getUser() == null
                || !product.getUser().getUserId().equals(authUtil.loggedInUserId()))) {
            throw new AccessDeniedException("You cannot manage another seller's product.");
        }
    }
}

package org.ecommerce.project.payload;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;
    @NotBlank
    @Size(min = 3, max = 160)
    private String productName;
    private String image;
    @NotBlank
    @Size(min = 6, max = 4000)
    private String description;
    @NotNull
    @Min(0)
    private Integer quantity;
    @Positive
    private double price;
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private double discount;
    private double specialPrice;
}

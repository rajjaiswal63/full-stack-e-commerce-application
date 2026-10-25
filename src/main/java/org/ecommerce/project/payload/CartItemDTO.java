package org.ecommerce.project.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    @NotNull
    private Long productId;
    @NotNull
    @Min(1)
    private Integer quantity;
}

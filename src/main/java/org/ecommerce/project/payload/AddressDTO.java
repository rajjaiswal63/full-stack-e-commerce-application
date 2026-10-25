package org.ecommerce.project.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Long addressId;
    @NotBlank
    @Size(min = 5, max = 255)
    private String street;
    @NotBlank
    @Size(min = 5, max = 255)
    private String buildingName;
    @NotBlank
    @Size(min = 4, max = 100)
    private String city;
    @NotBlank
    @Size(min = 2, max = 100)
    private String state;
    @NotBlank
    @Size(min = 2, max = 100)
    private String country;
    @NotBlank
    @Size(min = 5, max = 20)
    private String pincode;
}

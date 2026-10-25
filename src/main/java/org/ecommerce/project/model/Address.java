package org.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long addressId;

    @NotBlank
    @Size(min = 5)
    private String street;

    @NotBlank
    @Size(min = 5)
    private String buildingName;

    @NotBlank
    @Size(min = 4)
    private String city;

    @NotBlank
    @Size(min = 2)
    private String state;

    @NotBlank
    @Size(min = 2)
    private String country;

    @NotBlank
    @Size(min = 5)
    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Address(String street, String buildingName, String city, String state, String country, String pincode) {
        this.street = street;
        this.buildingName = buildingName;
        this.city = city;
        this.state = state;
        this.country = country;
        this.pincode = pincode;
    }
}

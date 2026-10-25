package org.ecommerce.project.controller;

import jakarta.validation.Valid;
import org.ecommerce.project.model.User;
import org.ecommerce.project.payload.AddressDTO;
import org.ecommerce.project.service.AddressService;
import org.ecommerce.project.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {
    private final AuthUtil authUtil;
    private final AddressService addressService;

    public AddressController(AuthUtil authUtil, AddressService addressService) {
        this.authUtil = authUtil;
        this.addressService = addressService;
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();
        return new ResponseEntity<>(addressService.createAddress(addressDTO, user), HttpStatus.CREATED);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        return ResponseEntity.ok(addressService.getUserAddresses(authUtil.loggedInUser()));
    }

    @GetMapping("/users/addresses")
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        return ResponseEntity.ok(addressService.getUserAddresses(authUtil.loggedInUser()));
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddressesById(addressId));
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long addressId,
                                                    @Valid @RequestBody AddressDTO addressDTO) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, addressDTO));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.deleteAddress(addressId));
    }

    @GetMapping("/admin/addresses")
    public ResponseEntity<List<AddressDTO>> getAllAddresses() {
        return ResponseEntity.ok(addressService.getAddresses());
    }
}

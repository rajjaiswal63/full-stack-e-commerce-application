package org.ecommerce.project.repository;

import org.ecommerce.project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserUserId(Long userId);
}

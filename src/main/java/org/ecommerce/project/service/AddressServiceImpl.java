package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.ecommerce.project.model.Address;
import org.ecommerce.project.model.User;
import org.ecommerce.project.payload.AddressDTO;
import org.ecommerce.project.repository.AddressRepository;
import org.ecommerce.project.repository.UserRepository;
import org.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    public AddressServiceImpl(AddressRepository addressRepository, ModelMapper modelMapper,
                              UserRepository userRepository, AuthUtil authUtil) {
        this.addressRepository = addressRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.authUtil = authUtil;
    }

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);
        address.setAddressId(null);
        address.setUser(user);
        user.getAddresses().add(address);
        Address savedAddress = addressRepository.save(address);
        userRepository.save(user);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        return addressRepository.findAll().stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO getAddressesById(Long addressId) {
        return modelMapper.map(findOwnedAddress(addressId), AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        return addressRepository.findByUserUserId(user.getUserId()).stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address address = findOwnedAddress(addressId);
        address.setCity(addressDTO.getCity());
        address.setPincode(addressDTO.getPincode());
        address.setState(addressDTO.getState());
        address.setCountry(addressDTO.getCountry());
        address.setStreet(addressDTO.getStreet());
        address.setBuildingName(addressDTO.getBuildingName());
        return modelMapper.map(addressRepository.save(address), AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        addressRepository.delete(findOwnedAddress(addressId));
        return "Address deleted successfully with addressId: " + addressId;
    }

    private Address findOwnedAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
        User currentUser = authUtil.loggedInUser();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (address.getUser() == null
                || !address.getUser().getUserId().equals(currentUser.getUserId()))) {
            throw new AccessDeniedException("You cannot access another user's address.");
        }
        return address;
    }
}

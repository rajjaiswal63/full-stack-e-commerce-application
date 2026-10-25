package org.ecommerce.project.service;

import org.ecommerce.project.model.AppRole;
import org.ecommerce.project.model.Role;
import org.ecommerce.project.model.User;
import org.ecommerce.project.payload.AuthenticationResult;
import org.ecommerce.project.payload.UserDTO;
import org.ecommerce.project.payload.UserResponse;
import org.ecommerce.project.repository.RoleRepository;
import org.ecommerce.project.repository.UserRepository;
import org.ecommerce.project.security.jwt.JwtUtils;
import org.ecommerce.project.security.request.LoginRequest;
import org.ecommerce.project.security.request.SignupRequest;
import org.ecommerce.project.security.response.MessageResponse;
import org.ecommerce.project.security.response.UserInfoResponse;
import org.ecommerce.project.security.security_services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public AuthenticationResult login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles);

        return new AuthenticationResult(response, jwtCookie);
    }

    @Override
    public ResponseEntity<MessageResponse> register(SignupRequest signUpRequest) {
        Set<String> requestedRoles = signUpRequest.getRole();
        boolean sellerRequested = requestedRoles != null && requestedRoles.stream()
                .filter(role -> role != null)
                .map(role -> role.toLowerCase().replace("role_", "").trim())
                .anyMatch("seller"::equals);
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (sellerRequested && !isAdmin) {
            throw new AccessDeniedException("Only an administrator can create a seller account.");
        }

        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new IllegalStateException("User role is missing."));
            roles.add(userRole);
        } else {
            for (String requestedRole : requestedRoles) {
                String normalizedRole = requestedRole == null
                        ? "" : requestedRole.toLowerCase().replace("role_", "").trim();
                if ("admin".equals(normalizedRole)) {
                    return ResponseEntity.badRequest().body(
                            new MessageResponse("Admin access cannot be requested during signup."));
                }
                AppRole role = switch (normalizedRole) {
                    case "seller" -> AppRole.ROLE_SELLER;
                    case "user" -> AppRole.ROLE_USER;
                    default -> null;
                };
                if (role == null) {
                    return ResponseEntity.badRequest().body(
                            new MessageResponse("Unsupported role: " + requestedRole));
                }
                Role savedRole = roleRepository.findByRoleName(role)
                        .orElseThrow(() -> new IllegalStateException("Required role is missing: " + role));
                roles.add(savedRole);
            }
        }

        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles);

        return response;
    }

    @Override
    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }

    @Override
    public UserResponse getAllSellers(Pageable pageable) {
        Page<User> allUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageable);
        List<UserDTO> userDtos = allUsers.getContent()
                .stream()
                .map(p -> modelMapper.map(p, UserDTO.class))
                .collect(Collectors.toList());

        UserResponse response = new UserResponse();
        response.setContent(userDtos);
        response.setPageNumber(allUsers.getNumber());
        response.setPageSize(allUsers.getSize());
        response.setTotalElements(allUsers.getTotalElements());
        response.setTotalPages(allUsers.getTotalPages());
        response.setLastPage(allUsers.isLast());
        return response;
    }


}

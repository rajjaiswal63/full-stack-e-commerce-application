package org.ecommerce.project.controller;

import jakarta.validation.Valid;
import org.ecommerce.project.jwt.JwtUtils;
import org.ecommerce.project.jwt.securityPayloads.LoginRequest;
import org.ecommerce.project.jwt.securityPayloads.MessageResponse;
import org.ecommerce.project.jwt.securityPayloads.SignupRequest;
import org.ecommerce.project.jwt.securityPayloads.UserInfoResponse;
import org.ecommerce.project.model.AppRole;
import org.ecommerce.project.model.Role;
import org.ecommerce.project.model.User;
import org.ecommerce.project.repository.RoleRepository;
import org.ecommerce.project.repository.UserRepository;
import org.ecommerce.project.security.security_services.UserDetailsImp;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils
    ,UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {

        System.out.println("1️⃣ SIGNIN HIT");

        try {

            System.out.println("2️⃣ Username: " + loginRequest.getUsername());
            System.out.println("3️⃣ Before authenticate");

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword()
                            )
                    );

            System.out.println("4️⃣ Authentication successful");

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImp userDetails =
                    (UserDetailsImp) authentication.getPrincipal();

            System.out.println("5️⃣ User: " + userDetails.getUsername());

//            String jwtToken =
//                    jwtUtils.generateTokenFromUsername(userDetails);

            //jtwToken to cookie
            ResponseCookie jwtCookie =
                    jwtUtils.generateJwtCookie(userDetails);

            System.out.println("6️⃣ JWT generated");

            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

//            UserInfoResponse response =
//                    new UserInfoResponse(
//                            userDetails.getId(),
//                            userDetails.getUsername(),
//                            roles,
//                            jwtToken
//                    );

            //jtwToken to cookie
            UserInfoResponse response =
                    new UserInfoResponse(
                            userDetails.getId(),
                            userDetails.getUsername(),
                            roles
                    );

            System.out.println("7️⃣ Returning response");

//            return ResponseEntity.ok(response);

            //for cookie
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                    jwtCookie.toString())
                    .body(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest){
        if(userRepository.existsByUsername(signupRequest.getUsername())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!") );
        }
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken!") );
        }

        User user = new User(signupRequest.getUsername(), signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()));

        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                String normalizedRole = role.toLowerCase().replace("role_", "");
                switch (normalizedRole) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/username")
    public String getCurrentUser(Authentication authentication){
        if(authentication == null){
            return "NULL";
        }
        return authentication.getName();
    }

    @GetMapping("/user")
    public ResponseEntity<?>getUserDetails(Authentication authentication){
        UserDetailsImp userDetails =
                (UserDetailsImp) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response =
                new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        roles
                );
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logout(){
        ResponseCookie jwtCookie =
                jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        jwtCookie.toString())
                .body(new MessageResponse("User logged out successfully!"));
    }
}

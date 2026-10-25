package org.ecommerce.project.security;

import jakarta.servlet.DispatcherType;
import org.ecommerce.project.model.AppRole;
import org.ecommerce.project.model.Role;
import org.ecommerce.project.model.User;
import org.ecommerce.project.repository.RoleRepository;
import org.ecommerce.project.repository.UserRepository;
import org.ecommerce.project.security.jwt.AuthEntryPointJwt;
import org.ecommerce.project.security.jwt.AuthTokenFilter;
import org.ecommerce.project.security.jwt.JwtUtils;
import org.ecommerce.project.security.security_services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(JwtUtils jwtUtils) {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider authenticationProvider, AuthTokenFilter authenticationJwtTokenFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/signin", "/api/auth/signup", "/api/auth/username").permitAll()
                        .requestMatchers("/api/auth/sellers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/order/razorpay/confirm").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/seller/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers("/api/public/**", "/images/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authenticationJwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        return http.build();
    }

    @Bean
    public CommandLineRunner initializeRolesAndOptionalAdmin(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:}") String adminUsername,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        return args -> {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_USER)));
            Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_SELLER)));
            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(AppRole.ROLE_ADMIN)));

            boolean anyAdminSetting = !adminUsername.isBlank() || !adminEmail.isBlank() || !adminPassword.isBlank();
            if (!anyAdminSetting) {
                return;
            }
            if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.length() < 12) {
                throw new IllegalStateException("Set all bootstrap admin values and use a password of at least 12 characters.");
            }

            User admin = userRepository.findByUserName(adminUsername)
                    .orElseGet(() -> new User(adminUsername, adminEmail, passwordEncoder.encode(adminPassword)));
            admin.setRoles(Set.of(userRole, sellerRole, adminRole));
            userRepository.save(admin);
        };
    }
}

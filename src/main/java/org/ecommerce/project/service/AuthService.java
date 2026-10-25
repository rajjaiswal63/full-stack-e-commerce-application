package org.ecommerce.project.service;

import org.ecommerce.project.payload.AuthenticationResult;
import org.ecommerce.project.payload.UserResponse;
import org.ecommerce.project.security.request.LoginRequest;
import org.ecommerce.project.security.request.SignupRequest;
import org.ecommerce.project.security.response.MessageResponse;
import org.ecommerce.project.security.response.UserInfoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface AuthService {

    AuthenticationResult login(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> register(SignupRequest signUpRequest);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    ResponseCookie logoutUser();

    UserResponse getAllSellers(Pageable pageable);
}

package org.ecommerce.project.payload;

import org.ecommerce.project.security.response.UserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseCookie;

@Data
@AllArgsConstructor
public class AuthenticationResult {
    private final UserInfoResponse response;
    private final ResponseCookie jwtCookie;
}

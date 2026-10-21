package com.collabflow.auth.service;

import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.UserResponse;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(String refreshToken);

    UserResponse me(UUID userId);
}

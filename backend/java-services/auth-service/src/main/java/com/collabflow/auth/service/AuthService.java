package com.collabflow.auth.service;

import com.collabflow.auth.dto.request.AdminResetPasswordRequest;
import com.collabflow.auth.dto.request.ChangePasswordRequest;
import com.collabflow.auth.dto.request.ImpersonateRequest;
import com.collabflow.auth.dto.request.ForgotPasswordRequest;
import com.collabflow.auth.dto.request.LoginRequest;
import com.collabflow.auth.dto.request.RefreshRequest;
import com.collabflow.auth.dto.request.RegisterRequest;
import com.collabflow.auth.dto.request.ResetPasswordRequest;
import com.collabflow.auth.dto.request.ResendVerificationRequest;
import com.collabflow.auth.dto.request.SelectOrganizationRequest;
import com.collabflow.auth.dto.request.UpdateProfileRequest;
import com.collabflow.auth.dto.request.VerifyEmailRequest;
import com.collabflow.auth.dto.response.AuthResponse;
import com.collabflow.auth.dto.response.ImpersonationResponse;
import com.collabflow.auth.dto.response.OrganizationSummary;
import com.collabflow.auth.dto.response.SessionResponse;
import com.collabflow.auth.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(String refreshToken);

    AuthResponse selectOrganization(UUID userId, SelectOrganizationRequest request);

    List<OrganizationSummary> listMyOrganizations(UUID userId);

    UserResponse me(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void resendVerification(ResendVerificationRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    List<SessionResponse> listSessions(UUID userId);

    void revokeAllSessions(UUID userId);

    void deactivate(UUID userId);

    boolean isMasterAdmin(UUID userId);

    void adminDeactivate(UUID adminId, UUID userId);

    void adminActivate(UUID adminId, UUID userId);

    void adminRevokeSessions(UUID adminId, UUID userId);

    void adminResetPassword(UUID adminId, UUID userId, AdminResetPasswordRequest request);

    String adminResendVerification(UUID adminId, UUID userId);

    ImpersonationResponse impersonate(UUID adminId, ImpersonateRequest request);

    void endImpersonation(UUID actorId, UUID targetId);
}

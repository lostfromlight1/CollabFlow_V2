package com.collabflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collabflow.auth")
public class AuthPhase2Properties {

    /** When false, public POST /register returns 403 (admin creates users instead). */
    private boolean publicRegisterEnabled = false;

    /** Seeded on boot if missing (master admin portal). */
    private String masterAdminEmail = "admin@collabflow.local";
    private String masterAdminPassword = "ChangeMeAdmin123!";
    private String masterAdminName = "Master Admin";

    private int verifyTokenHours = 48;
    private int resetTokenHours = 2;

    /** App base URL used in verify/reset email body links (stub). */
    private String publicAppBaseUrl = "http://localhost:3000";

    public boolean isPublicRegisterEnabled() {
        return publicRegisterEnabled;
    }

    public void setPublicRegisterEnabled(boolean publicRegisterEnabled) {
        this.publicRegisterEnabled = publicRegisterEnabled;
    }

    public String getMasterAdminEmail() {
        return masterAdminEmail;
    }

    public void setMasterAdminEmail(String masterAdminEmail) {
        this.masterAdminEmail = masterAdminEmail;
    }

    public String getMasterAdminPassword() {
        return masterAdminPassword;
    }

    public void setMasterAdminPassword(String masterAdminPassword) {
        this.masterAdminPassword = masterAdminPassword;
    }

    public String getMasterAdminName() {
        return masterAdminName;
    }

    public void setMasterAdminName(String masterAdminName) {
        this.masterAdminName = masterAdminName;
    }

    public int getVerifyTokenHours() {
        return verifyTokenHours;
    }

    public void setVerifyTokenHours(int verifyTokenHours) {
        this.verifyTokenHours = verifyTokenHours;
    }

    public int getResetTokenHours() {
        return resetTokenHours;
    }

    public void setResetTokenHours(int resetTokenHours) {
        this.resetTokenHours = resetTokenHours;
    }

    public String getPublicAppBaseUrl() {
        return publicAppBaseUrl;
    }

    public void setPublicAppBaseUrl(String publicAppBaseUrl) {
        this.publicAppBaseUrl = publicAppBaseUrl;
    }
}

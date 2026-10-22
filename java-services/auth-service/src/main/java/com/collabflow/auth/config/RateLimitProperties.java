package com.collabflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collabflow.rate-limit")
public class RateLimitProperties {

    /** Max login attempts per client key within the window. */
    private int loginMaxAttempts = 10;
    private int loginWindowSeconds = 60;

    /** Max register attempts per client key within the window. */
    private int registerMaxAttempts = 5;
    private int registerWindowSeconds = 3600;

    private int refreshMaxAttempts = 30;
    private int refreshWindowSeconds = 60;

    private int resendVerifyMaxAttempts = 5;
    private int resendVerifyWindowSeconds = 3600;

    private int forgotPasswordMaxAttempts = 5;
    private int forgotPasswordWindowSeconds = 3600;

    public int getLoginMaxAttempts() {
        return loginMaxAttempts;
    }

    public void setLoginMaxAttempts(int loginMaxAttempts) {
        this.loginMaxAttempts = loginMaxAttempts;
    }

    public int getLoginWindowSeconds() {
        return loginWindowSeconds;
    }

    public void setLoginWindowSeconds(int loginWindowSeconds) {
        this.loginWindowSeconds = loginWindowSeconds;
    }

    public int getRegisterMaxAttempts() {
        return registerMaxAttempts;
    }

    public void setRegisterMaxAttempts(int registerMaxAttempts) {
        this.registerMaxAttempts = registerMaxAttempts;
    }

    public int getRegisterWindowSeconds() {
        return registerWindowSeconds;
    }

    public void setRegisterWindowSeconds(int registerWindowSeconds) {
        this.registerWindowSeconds = registerWindowSeconds;
    }

    public int getRefreshMaxAttempts() {
        return refreshMaxAttempts;
    }

    public void setRefreshMaxAttempts(int refreshMaxAttempts) {
        this.refreshMaxAttempts = refreshMaxAttempts;
    }

    public int getRefreshWindowSeconds() {
        return refreshWindowSeconds;
    }

    public void setRefreshWindowSeconds(int refreshWindowSeconds) {
        this.refreshWindowSeconds = refreshWindowSeconds;
    }

    public int getResendVerifyMaxAttempts() {
        return resendVerifyMaxAttempts;
    }

    public void setResendVerifyMaxAttempts(int resendVerifyMaxAttempts) {
        this.resendVerifyMaxAttempts = resendVerifyMaxAttempts;
    }

    public int getResendVerifyWindowSeconds() {
        return resendVerifyWindowSeconds;
    }

    public void setResendVerifyWindowSeconds(int resendVerifyWindowSeconds) {
        this.resendVerifyWindowSeconds = resendVerifyWindowSeconds;
    }

    public int getForgotPasswordMaxAttempts() {
        return forgotPasswordMaxAttempts;
    }

    public void setForgotPasswordMaxAttempts(int forgotPasswordMaxAttempts) {
        this.forgotPasswordMaxAttempts = forgotPasswordMaxAttempts;
    }

    public int getForgotPasswordWindowSeconds() {
        return forgotPasswordWindowSeconds;
    }

    public void setForgotPasswordWindowSeconds(int forgotPasswordWindowSeconds) {
        this.forgotPasswordWindowSeconds = forgotPasswordWindowSeconds;
    }
}

package com.collabflow.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collabflow.platform")
public class PlatformProperties {

    /**
     * When false (Phase 2 default), {@code POST /api/v1/organizations} requires master admin.
     * Set true only for local demos that still want self-serve org create.
     */
    private boolean selfServeOrgCreate = false;

    public boolean isSelfServeOrgCreate() {
        return selfServeOrgCreate;
    }

    public void setSelfServeOrgCreate(boolean selfServeOrgCreate) {
        this.selfServeOrgCreate = selfServeOrgCreate;
    }
}

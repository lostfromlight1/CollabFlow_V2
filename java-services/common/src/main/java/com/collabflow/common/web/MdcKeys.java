package com.collabflow.common.web;

/** SLF4J MDC keys used across Java services (see logging pattern {@code %X{apiId}}). */
public final class MdcKeys {

    public static final String API_ID = "apiId";
    public static final String USER_ID = "userId";

    private MdcKeys() {}
}

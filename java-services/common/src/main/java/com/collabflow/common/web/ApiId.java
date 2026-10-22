package com.collabflow.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stable endpoint id for logs / support (MDC key {@link MdcKeys#API_ID}).
 * Convention: {@code AREA-NNN} e.g. {@code AUTH-006}, {@code ORG-001}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiId {

    /** Endpoint id, e.g. {@code ORG-001}. */
    String value();
}

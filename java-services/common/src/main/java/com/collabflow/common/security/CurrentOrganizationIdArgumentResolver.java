package com.collabflow.common.security;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentOrganizationId} from the {@code X-Organization-Id} request header.
 */
public class CurrentOrganizationIdArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER_NAME = "X-Organization-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOrganizationId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String raw = webRequest.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + HEADER_NAME + ": expected UUID");
        }
    }
}

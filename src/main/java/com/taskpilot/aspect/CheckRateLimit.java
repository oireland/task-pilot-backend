package com.taskpilot.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method-level annotation to indicate that an endpoint is subject to
 * rate limiting checks. The RateLimitingAspect will intercept methods
 * marked with this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckRateLimit {
}
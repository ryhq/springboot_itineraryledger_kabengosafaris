package com.itineraryledger.kabengosafaris.Public.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field in a DTO as translatable.
 * The PublicTranslationService will translate annotated fields
 * when a non-English locale is requested via the Accept-Language header.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Translatable {
}

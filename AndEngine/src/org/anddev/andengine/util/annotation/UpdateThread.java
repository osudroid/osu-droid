// osu!droid ONLY
package org.anddev.andengine.util.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method or constructor, or every method of the annotated class, should only be called on
 * the update thread, and that the annotated field should only be accessed on the update thread.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
public @interface UpdateThread {
}

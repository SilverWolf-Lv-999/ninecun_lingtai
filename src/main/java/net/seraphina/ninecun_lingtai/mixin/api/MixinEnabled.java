package net.seraphina.ninecun_lingtai.mixin.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Enables a mixin only when its declared environment condition is satisfied. */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MixinEnabled {
    /**
     * The condition under which the mixin is enabled.
     */
    Type type() default Type.FOREVER;

    /**
     * The mod ID used by {@link Type#MOD_LOAD} and {@link Type#UN_MOD_LOAD}.
     */
    String modId() default "minecraft";
}

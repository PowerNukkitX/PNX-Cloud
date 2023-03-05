package cn.powernukkitx.cloud.util;

import io.micronaut.cache.annotation.CacheAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@CacheAnnotation
public @interface CacheKeyPrefix {
    /**
     * The prefix of the cache key. If this is not specified, the cache key prefix will be the name of the method.
     *
     * @return The prefix of the cache key
     */
    @Nullable
    String value() default "";
}

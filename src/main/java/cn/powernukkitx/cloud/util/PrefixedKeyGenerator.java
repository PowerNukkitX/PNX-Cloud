package cn.powernukkitx.cloud.util;

import io.micronaut.aop.chain.MethodInterceptorChain;
import io.micronaut.cache.interceptor.CacheKeyGenerator;
import io.micronaut.cache.interceptor.ParametersKey;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Introspected;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Introspected
public class PrefixedKeyGenerator implements CacheKeyGenerator {
    @Override
    public Object generateKey(AnnotationMetadata annotationMetadata, Object... params) {
        return makeKey(annotationMetadata, params);
    }

    @Contract("_, _ -> new")
    private @NotNull Object makeKey(@NotNull AnnotationMetadata annotationMetadata, Object... params) {
        var prefix = annotationMetadata.getAnnotation(CacheKeyPrefix.class);
        if (prefix == null) {
            return new ParametersKey(params);
        }
        var expandParams = new Object[params.length + 1];
        var keyPrefix = prefix.stringValue("value").orElse(null);
        if ((keyPrefix == null || keyPrefix.isBlank()) && annotationMetadata instanceof MethodInterceptorChain<?,?> chain) {
            keyPrefix = chain.getMethodName();
        }
        expandParams[0] = keyPrefix;
        if (params.length > 0) {
            System.arraycopy(params, 0, expandParams, 1, params.length);
        }
        return new ParametersKey(expandParams);
    }
}

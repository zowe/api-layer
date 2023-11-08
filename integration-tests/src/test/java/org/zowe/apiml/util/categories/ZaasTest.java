package org.zowe.apiml.util.categories;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * For testing ZAAS features
 * It contains all authentication features enabled and the typical z/OSMF configuration.
 * It can be reused also for other testing. Rename this tag if reused for other purposes.
 */
@Tag("ZaasTest")
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ZaasTest {
}

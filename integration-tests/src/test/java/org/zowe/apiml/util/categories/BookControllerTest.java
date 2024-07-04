package org.zowe.apiml.util.categories;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Tag("BookControllerTest")
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BookControllerTest {
}

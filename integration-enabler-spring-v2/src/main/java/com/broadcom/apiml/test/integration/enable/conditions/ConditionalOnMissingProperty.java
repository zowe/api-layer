/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.enable.conditions;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(ConditionalOnMissingProperty.OnMissingPropertyCondition.class)
public @interface ConditionalOnMissingProperty {

    String[] value();

    @Order(Ordered.HIGHEST_PRECEDENCE + 40)
    @SuppressWarnings({"Duplicates", "squid:S134"})
    class OnMissingPropertyCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            MultiValueMap<String, Object> annotationAttributes
                = metadata.getAllAnnotationAttributes(ConditionalOnMissingProperty.class.getName());
            if (!annotationAttributes.isEmpty()) {
                ConditionOutcome conditionOutcome = null;
                for (Object values : annotationAttributes.get("value")) {
                    for (String propertyName : (String[]) values) {
                        if (context.getEnvironment().containsProperty(propertyName)) {
                            conditionOutcome = checkPropertyValueForNullOrEmpty(context, propertyName);
                        }
                        if (conditionOutcome != null && !conditionOutcome.isMatch()) {
                            return ConditionOutcome.noMatch(ConditionMessage.of("At least one property with a value was found."));
                        }
                    }
                }
            }

            // return match if no matching property was found:
            return ConditionOutcome.match(ConditionMessage.of("None of the given properties found"));
        }

        private ConditionOutcome checkPropertyValueForNullOrEmpty(ConditionContext context, String propertyName) {
            ConditionOutcome conditionOutcome;
            try {
                conditionOutcome = checkPropertyContentsForEmptyMatch(context, propertyName);
            } catch (Exception e) {
                conditionOutcome = ConditionOutcome.match(ConditionMessage.of("Property found"));
            }
            return conditionOutcome;
        }

        /**
         * Check the value of the property, an empty or null value should be considered as a no match
         *
         * @param context      condition context
         * @param propertyName the property
         * @return not match found or match with a value
         */
        private ConditionOutcome checkPropertyContentsForEmptyMatch(ConditionContext context, String propertyName) {
            String propertyValue = context.getEnvironment().getProperty(propertyName, "");
            if (!propertyValue.isEmpty()) {
                // return NO match if there is a property of the given name with a non null/non empty value
                return ConditionOutcome.noMatch(ConditionMessage.of("Found property " + propertyName + " with value: " + propertyValue));
            } else {
                return ConditionOutcome.match(ConditionMessage.of("Found property " + propertyName + " with an empty/null value"));
            }
        }
    }
}

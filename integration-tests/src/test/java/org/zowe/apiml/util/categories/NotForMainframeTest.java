/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.categories;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
/**
 * A category marker for tests which must run that can`t be run on mainframe.
 *
 * These tests will run by default with other integration tests.
 * To run locally, use runLocalIntegrationTests Gradle task.
 */
@Tag("NotForMainframeTest")
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotForMainframeTest {
}

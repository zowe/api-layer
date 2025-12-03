/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.logging;

import ch.qos.logback.core.boolex.PropertyConditionBase;
import lombok.Getter;
import lombok.Setter;

public class PropertyContainsCondition extends PropertyConditionBase {

    @Setter
    @Getter
    String key;

    @Setter
    @Getter
    String value;

    @Override
    public void start() {
        if (key == null) {
            addError("In PropertyContainsValue 'key' parameter cannot be null");
            return;
        }
        if (value == null) {
            addError("In PropertyContainsValue 'value' parameter cannot be null");
            return;
        }
        super.start();
    }

    @Override
    public boolean evaluate() {
        if (key == null) {
            addError("key cannot be null");
            return false;
        }

        String val = p(key);
        if (val == null)
            return false;
        else {
            return val.contains(value);
        }

    }

}

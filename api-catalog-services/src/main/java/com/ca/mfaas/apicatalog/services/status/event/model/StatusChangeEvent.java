/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status.event.model;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.util.TimeZone;

public interface StatusChangeEvent {

    /**
     * Create a String time stamp for this event
     *
     * @return a timestamp
     */
    default String setTimeStamp() {
        Instant now = Instant.now();
        Timestamp current = Timestamp.from(now);
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(current);
    }
}

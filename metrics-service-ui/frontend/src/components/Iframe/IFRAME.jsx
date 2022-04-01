/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React from 'react';

export function IFRAME() {
    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
        }}
            dangerouslySetInnerHTML={{
                __html: "<iframe src='http://localhost:8080/d-solo/FZmhyNPnz/new-dashboard?orgId=1&from=1647347602192&to=1647369202192&panelId=2' width='1000' height='600' />",
            }}
        />
    );
}
export default IFRAME;

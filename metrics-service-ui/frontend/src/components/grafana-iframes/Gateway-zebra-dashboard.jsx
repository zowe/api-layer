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

export function GatewayZebraDashboard() {
    return (
        <fragment>
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '2rem',
                }}
            >
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=2' width='450' height='300' />",
                    }}
                />
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=3'  width='450' height='300' />",
                    }}
                />
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=12'  width='450' height='300' />",
                    }}
                />
            </div>
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '2rem',
                }}
            >
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=14'  width='450' height='300' />",
                    }}
                />
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=15'  width='450' height='300' />",
                    }}
                />
                <div
                    dangerouslySetInnerHTML={{
                        __html: "<iframe src='http://localhost:3000/d-solo/0MIkGHU7z/rprt-cpc-metrics?orgId=1&panelId=19'  width='450' height='300' />",
                    }}
                />
            </div>
        </fragment>
    );
}

export default GatewayZebraDashboard;

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
        <fragment>
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
        }}>
            <div       dangerouslySetInnerHTML={{
                 __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649169576404&to=1649169876405&panelId=10' width='450' height='200' />",
            }} />
            <div       dangerouslySetInnerHTML={{
                __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649178335450&to=1649178635450&panelId=12' width='250' height='100' />",
            }} />
            <div       dangerouslySetInnerHTML={{
                __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649178445869&to=1649178745869&panelId=14' width='250' height='100' />",
            }} />
            <div       dangerouslySetInnerHTML={{
                __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649178527343&to=1649178827343&panelId=13' width='450' height='200' />",
            }} />
        </div>
            <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
            }}>
                <div       dangerouslySetInnerHTML={{
                    __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649179375601&to=1649179675601&panelId=24' width='450' height='200' />",
                }} />
            </div>

        </fragment>
    );
}
export default IFRAME;

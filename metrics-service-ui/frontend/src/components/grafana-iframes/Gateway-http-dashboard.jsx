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

export function GatewayHttpDashboard() {
    return (
        <fragment>
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2rem',
        }}>
            <div       dangerouslySetInnerHTML={{
                 __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649169576404&to=1649169876405&panelId=10' width='500' height='400' />",
            }} />
            <div       dangerouslySetInnerHTML={{
                __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649815800739&to=1649816100739&panelId=24'  width='500' height='400' />",
            }} />

        </div>
            <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '2rem',
            }}>
                <div       dangerouslySetInnerHTML={{
                    __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649178335450&to=1649178635450&panelId=12' width='250' height='200' />",
                }} />
                <div       dangerouslySetInnerHTML={{
                    __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649815030049&to=1649815330049&panelId=23' width='250' height='200' />",
                }} />
                <div       dangerouslySetInnerHTML={{
                    __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649815865502&to=1649816165502&panelId=22'  width='250' height='200' />",
                }} />
                <div       dangerouslySetInnerHTML={{
                    __html: "<iframe src='http://localhost:8080/d-solo/JyDukHP7k/apiml-metrics-service-http?orgId=1&from=1649815895850&to=1649816195850&panelId=21'  width='250' height='200' />",
                }} />
            </div>

        </fragment>
    );
}
export default GatewayHttpDashboard;

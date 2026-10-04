/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { useEffect, useState } from 'react';
import { Container } from '@material-ui/core';
import './footer.css';
import { getGatewayUrl } from '../../helpers/urls';

const GATEWAY_VERSION_ENDPOINT = '/gateway/version';

/**
 * Format the version the same way the Gateway homepage does, so both pages stay in sync.
 * @param versionInfo version payload returned by the Gateway /version endpoint
 * @returns the formatted version, or null when the payload carries no usable version
 */
const formatGatewayVersion = (versionInfo) => {
    const apiml = versionInfo?.apiml;
    if (!apiml?.version) {
        return null;
    }
    return `API ML Version ${apiml.version} build # ${apiml.buildNumber}`;
};

/**
 * Footer showing the version of the running API Mediation Layer. The version is read
 * from the Gateway /version endpoint at runtime instead of a build time constant, so
 * the login page always matches the version shown on the Gateway homepage. When the
 * Gateway is not reachable the build time value is kept as a fallback.
 */
function Footer() {
    const [version, setVersion] = useState(import.meta.env.VITE_ZOWE_BUILD_INFO);

    useEffect(() => {
        let cancelled = false;
        fetch(`${getGatewayUrl()}${GATEWAY_VERSION_ENDPOINT}`)
            .then((response) => response.json())
            .then((versionInfo) => {
                const gatewayVersion = formatGatewayVersion(versionInfo);
                if (!cancelled && gatewayVersion) {
                    setVersion(gatewayVersion);
                }
            })
            .catch(() => {
                // keep the build time value when the Gateway is not reachable
            });
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <footer id="pageFooter">
            <Container>{version}</Container>
        </footer>
    );
}

export default Footer;

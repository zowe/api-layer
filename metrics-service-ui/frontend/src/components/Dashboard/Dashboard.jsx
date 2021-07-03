/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import Typography from '@material-ui/core/Typography';
import HystrixCommandMonitor from './HystrixCommandMonitor';

export default function Dashboard() {
    /* eslint-disable-next-line */
    let hystrixMonitor = new HystrixCommandMonitor(0, 'dependencies', {});
    return (
        <Typography id="name" variant="h2" component="h1" gutterBottom align="center">
            Metrics Service
        </Typography>
    );
}

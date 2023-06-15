/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const routes = require('./routes/routes').router;

const app = express();

const corsOptions = {
    origin: 'https://localhost:3000',
    credentials: true,
};

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(cors(corsOptions));

routes(app);

const server = app.listen(8000, () => {
    // eslint-disable-next-line no-console
    console.debug(`Mocked backend running on port - ${server.address().port}`);
});

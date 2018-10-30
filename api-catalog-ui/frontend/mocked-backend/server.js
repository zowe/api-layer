// tslint:disable:no-console
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const routes = require('./routes/routes').router;

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(cors());

routes(app);

const server = app.listen(8000, () => {
    console.debug(`Mocked backend running on port - ${server.address().port}`);
});

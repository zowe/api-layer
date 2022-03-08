const path = require('path');
const fs = require('fs');

const loginSuccess = require('./assets/login-success.json');
const invalidCredentials = require('./assets/invalid-credentials.json');
const timeout = require('./assets/timeout-error.json');
const clusters = require('./assets/services/clusters.json');

const metrics = fs.readFileSync(path.join(__dirname, './assets/services/metrics.txt'), 'utf-8');

function validateCredentials({ username, password }) {
    return username === 'USER' && password === 'validPassword';
}

function returnTimeout({ username }) {
    return username === 'timeout';
}

const appRouter = app => {
    app.post('/metrics-service/api/v1/auth/login', async (req, res) => {
        const credentials = req.body;
        if (returnTimeout(credentials)) {
            res.status(500).send(timeout);
        } else if (validateCredentials(credentials)) {
            console.log('LOGIN');
            setTimeout(() => res.status(204).send(loginSuccess), 2000);
        }
        else {
            console.log(invalidCredentials);
            res.status(401).send(invalidCredentials);
        }
    });

    app.post('/metrics-service/api/v1/auth/logout', (req, res) => {
        res.status(200).send(loginSuccess);
    });

    app.get('/gateway/api/v1/auth/query', (req, res) => {
        res.status(200).send();
    });

    app.get('/metrics-service/api/v1/clusters', (req, res) => {
        res.status(200).send(clusters);
    });

    app.get('/metrics-service/sse/v1/turbine.stream', (req, res) => {
        console.log('METRICS STREAM OPENED');

        res.setHeader('Content-Type', 'text/event-stream');
        const responseIntervalId = setInterval(() => {
            res.write(metrics);
        }, 1000);

        res.on('close', () => {
            console.log('METRICS STREAM CLOSED');
            clearInterval(responseIntervalId);
            res.end();
        });
    });
};

module.exports = {
    router: appRouter,
};

const loginSuccess = require('./assets/login-success.json');
const invalidCredentials = require('./assets/invalid-credentials.json');
const timeout = require('./assets/timeout-error.json');

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
        res.status(200).send(loginSuccess);
    });
};

module.exports = {
    router: appRouter,
};

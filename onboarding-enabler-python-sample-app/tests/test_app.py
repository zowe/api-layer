"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""

import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, mock_open
import json

mock_swagger_json = json.dumps({"swagger": "2.0", "info": {"version": "1.0.0"}})

@pytest.fixture(scope="module")
def mock_enabler():
    """Mock PythonEnabler"""
    with patch("zowe_apiml_onboarding_enabler_python.registration.PythonEnabler") as mock_class:
        instance = mock_class.return_value
        instance.register.return_value = None
        instance.unregister.return_value = None
        instance.ssl_config = {
            "certificate": "/dev/null",
            "keystore": "/dev/null",
            "caFile": "/dev/null"
        }
        yield instance

@pytest.fixture(scope="module")
def mock_ssl():
    with patch("ssl.SSLContext.load_cert_chain"):
        yield


@pytest.fixture(scope="module")
def client(mock_enabler, mock_ssl):
    with patch("builtins.open", mock_open(read_data="ssl:\n  certificate: /dev/null\n  keystore: /dev/null\n")):
        from src.app import app
        return TestClient(app)


def test_register_python_enabler(client, mock_enabler):
    response = client.get("/pythonservice/registerInfo")
    assert response.status_code == 200
    assert response.json() == {"message": "Registered with Python eureka client to Discovery service"}
    mock_enabler.register.assert_called_once()


def test_unregister_python_enabler(client, mock_enabler):
    response = client.get("/pythonservice/unregisterInfo")
    assert response.status_code == 200
    assert response.json() == {"message": "Unregistered Python eureka client from Discovery service"}
    mock_enabler.unregister.assert_called_once()


def test_hello_endpoint(client):
    response = client.get("/pythonservice/hello")
    assert response.status_code == 200
    assert response.json() == {"message": "Hello world in swagger"}


def test_get_swagger_endpoint(client):
    with patch("builtins.open", mock_open(read_data=mock_swagger_json)):
        response = client.get("/pythonservice/apidoc")
    assert response.status_code == 200
    assert response.json() == {"swagger": "2.0", "info": {"version": "1.0.0"}}


def test_application_info_endpoint(client):
    response = client.get("/pythonservice/application/info")
    assert response.status_code == 200
    data = response.json()
    assert "build" in data
    assert data["build"]["name"] == "python-service"


def test_application_health_endpoint(client):
    response = client.get("/pythonservice/application/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}

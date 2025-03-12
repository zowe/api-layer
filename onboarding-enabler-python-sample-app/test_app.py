import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock, mock_open
import yaml
import json

from app import app

# Mock content for pythonSwagger.json
mock_swagger_json = json.dumps({"swagger": "2.0", "info": {"version": "1.0.0"}})

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def mock_enabler():
    with patch("app.enabler") as mock:
        mock.register = MagicMock()
        mock.unregister = MagicMock()
        yield mock


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

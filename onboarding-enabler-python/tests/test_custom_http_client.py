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
from unittest.mock import patch, MagicMock, AsyncMock
import ssl
import yaml
from src.zowe_apiml_onboarding_enabler_python.custom_http_client import HttpResponse, HttpClient

mock_yaml_content = """
eureka:
  ssl: true
  host: localhost
  ipAddress: 127.0.0.1
  port: 10011

instance:
  app: pythonservice

ssl:
  certificate: "../keystore/localhost/localhost.keystore.cer"
  keystore: "../keystore/localhost/localhost.keystore.key"
  caFile: "../keystore/localhost/localhost.pem"
  keyPassword: "password"
"""

@pytest.fixture
def mock_config_loader():
    mocked_yaml = yaml.safe_load(mock_yaml_content)

    def mock_init(self, *args, **kwargs):
        self.config = mocked_yaml

    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.ConfigLoader.__init__", mock_init), \
        patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.config_loader.config", mocked_yaml):
        yield

@pytest.fixture
def mock_ssl_context():
    mock_ssl = MagicMock(spec=ssl.SSLContext)

    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.ssl.create_default_context", return_value=mock_ssl):
        yield mock_ssl


@pytest.fixture
def mock_aiohttp_session():
    with patch("src.zowe_apiml_onboarding_enabler_python.custom_http_client.aiohttp.ClientSession") as mock_session_cls:
        mock_session = MagicMock()
        mock_response_context = MagicMock()
        mock_response = MagicMock()

        mock_response.status = 200
        mock_response.text = AsyncMock(return_value="success")
        mock_response.headers = {'Content-Type': 'application/xml'}

        mock_response_context.__aenter__.return_value = mock_response
        mock_session.request.return_value = mock_response_context
        mock_session_cls.return_value.__aenter__.return_value = mock_session

        yield mock_session

@pytest.mark.asyncio
async def test_http_client_ssl_setup(mock_config_loader, mock_ssl_context, mock_aiohttp_session):
    client = HttpClient()
    request_mock = MagicMock()
    request_mock.url = "https://example.com"

    response = await client.urlopen(request_mock, data=None)

    mock_ssl_context.load_cert_chain.assert_called_once_with(
        certfile="../keystore/localhost/localhost.keystore.cer",
        keyfile="../keystore/localhost/localhost.keystore.key",
        password="password"
    )

    assert isinstance(response, HttpResponse)
    assert response.raw_response.status == 200
    assert response.body_text == "success"

def test_http_response():
    raw_response_mock = MagicMock()
    raw_response_mock.status = 200
    response = HttpResponse(raw_response_mock, "success")

    assert response.raw_response.status == 200
    assert response.body_text == "success"
